package com.metriclab.service;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.IntersectionTypeTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class JavaAstSupport {

    private JavaAstSupport() {
    }

    static ParsedSource parse(String fileName, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null || source == null || source.isBlank()) {
            return null;
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavaFileObject fileObject = new StringJavaFileObject(fileName, source);
            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    null,
                    List.of("-proc:none", "-Xlint:-options"),
                    null,
                    List.of(fileObject)
            );
            var iterator = task.parse().iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            var unit = iterator.next();
            Trees trees = Trees.instance(task);
            SourcePositions positions = trees.getSourcePositions();
            AstScanner scanner = new AstScanner(unit, positions);
            scanner.scan(unit, null);
            if (scanner.types.isEmpty()) {
                return null;
            }
            return new ParsedSource(fileName, List.copyOf(scanner.types));
        } catch (IOException exception) {
            return null;
        }
    }

    private static final class AstScanner extends TreePathScanner<Void, Void> {

        private final com.sun.source.tree.CompilationUnitTree unit;
        private final SourcePositions positions;
        private final ArrayDeque<TypeInfo> typeStack = new ArrayDeque<>();
        private final List<TypeInfo> types = new ArrayList<>();

        private AstScanner(com.sun.source.tree.CompilationUnitTree unit, SourcePositions positions) {
            this.unit = unit;
            this.positions = positions;
        }

        @Override
        public Void visitClass(ClassTree node, Void unused) {
            String name = node.getSimpleName().toString();
            if (name == null || name.isBlank()) {
                return super.visitClass(node, unused);
            }
            TypeInfo typeInfo = new TypeInfo(
                    name,
                    normalizeType(node.getKind()),
                    simpleTypeName(node.getExtendsClause()),
                    node.getImplementsClause().stream().map(JavaAstSupport::simpleTypeName).filter(value -> !value.isBlank()).toList(),
                    startLine(node),
                    endLine(node),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new LinkedHashSet<>()
            );
            typeInfo.referencedTypes.addAll(extractTypeNames(node.getExtendsClause()));
            for (Tree implemented : node.getImplementsClause()) {
                typeInfo.referencedTypes.addAll(extractTypeNames(implemented));
            }
            types.add(typeInfo);
            typeStack.push(typeInfo);
            super.visitClass(node, unused);
            typeStack.pop();
            return null;
        }

        @Override
        public Void visitVariable(VariableTree node, Void unused) {
            TypeInfo currentType = typeStack.peek();
            if (currentType != null && getCurrentPath().getParentPath() != null
                    && getCurrentPath().getParentPath().getLeaf() instanceof ClassTree) {
                String fieldName = node.getName().toString();
                if (fieldName != null && !fieldName.isBlank()) {
                    currentType.fields.add(new FieldInfo(fieldName));
                }
                currentType.referencedTypes.addAll(extractTypeNames(node.getType()));
            }
            return super.visitVariable(node, unused);
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            TypeInfo currentType = typeStack.peek();
            if (currentType == null) {
                return super.visitMethod(node, unused);
            }
            boolean constructor = node.getName().contentEquals("<init>");
            String methodName = node.getName().contentEquals("<init>") ? currentType.name : node.getName().toString();
            int startLine = startLine(node);
            int endLine = endLine(node);
            boolean executable = node.getBody() != null;
            int complexity = executable ? new ComplexityCounter().count(node) : 0;
            Set<String> fieldReferences = executable ? defaultIfNull(new FieldReferenceScanner().scan(node.getBody(), null)) : Set.of();
            Set<String> localTypeReferences = executable ? defaultIfNull(new TypeReferenceScanner().scan(node.getBody(), null)) : Set.of();
            Set<String> invokedMethods = executable ? defaultIfNull(new InvocationScanner().scan(node.getBody(), null)) : Set.of();

            currentType.referencedTypes.addAll(extractTypeNames(node.getReturnType()));
            if (!constructor) {
                for (VariableTree parameter : node.getParameters()) {
                    currentType.referencedTypes.addAll(extractTypeNames(parameter.getType()));
                }
            }
            for (Tree thrown : node.getThrows()) {
                currentType.referencedTypes.addAll(extractTypeNames(thrown));
            }
            currentType.referencedTypes.addAll(localTypeReferences);
            currentType.methods.add(new MethodInfo(methodName, startLine, endLine, executable, complexity, fieldReferences, invokedMethods));
            return super.visitMethod(node, unused);
        }

        private int startLine(Tree tree) {
            long start = positions.getStartPosition(unit, tree);
            if (start == Diagnostic.NOPOS) {
                return 1;
            }
            return (int) unit.getLineMap().getLineNumber(start);
        }

        private int endLine(Tree tree) {
            long end = positions.getEndPosition(unit, tree);
            if (end == Diagnostic.NOPOS) {
                return startLine(tree);
            }
            return (int) unit.getLineMap().getLineNumber(Math.max(0, end - 1));
        }
    }

    private static final class ComplexityCounter extends TreeScanner<Void, Void> {

        private int complexity = 1;

        int count(MethodTree methodTree) {
            if (methodTree.getBody() != null) {
                scan(methodTree.getBody(), null);
            }
            return complexity;
        }

        @Override
        public Void visitIf(com.sun.source.tree.IfTree node, Void unused) {
            complexity++;
            return super.visitIf(node, unused);
        }

        @Override
        public Void visitForLoop(ForLoopTree node, Void unused) {
            complexity++;
            return super.visitForLoop(node, unused);
        }

        @Override
        public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void unused) {
            complexity++;
            return super.visitEnhancedForLoop(node, unused);
        }

        @Override
        public Void visitWhileLoop(WhileLoopTree node, Void unused) {
            complexity++;
            return super.visitWhileLoop(node, unused);
        }

        @Override
        public Void visitDoWhileLoop(DoWhileLoopTree node, Void unused) {
            complexity++;
            return super.visitDoWhileLoop(node, unused);
        }

        @Override
        public Void visitCase(CaseTree node, Void unused) {
            if (!node.getExpressions().isEmpty()) {
                complexity++;
            }
            return super.visitCase(node, unused);
        }

        @Override
        public Void visitCatch(CatchTree node, Void unused) {
            complexity++;
            return super.visitCatch(node, unused);
        }

        @Override
        public Void visitConditionalExpression(ConditionalExpressionTree node, Void unused) {
            complexity++;
            return super.visitConditionalExpression(node, unused);
        }

        @Override
        public Void visitBinary(BinaryTree node, Void unused) {
            if (node.getKind() == Tree.Kind.CONDITIONAL_AND || node.getKind() == Tree.Kind.CONDITIONAL_OR) {
                complexity++;
            }
            return super.visitBinary(node, unused);
        }
    }

    private static final class FieldReferenceScanner extends TreeScanner<Set<String>, Void> {

        @Override
        public Set<String> reduce(Set<String> left, Set<String> right) {
            if (left == null || left.isEmpty()) {
                return right == null ? new LinkedHashSet<>() : right;
            }
            if (right != null) {
                left.addAll(right);
            }
            return left;
        }

        @Override
        public Set<String> visitIdentifier(IdentifierTree node, Void unused) {
            Set<String> result = new LinkedHashSet<>();
            result.add(node.getName().toString());
            return result;
        }

        @Override
        public Set<String> visitMemberSelect(MemberSelectTree node, Void unused) {
            Set<String> result = scan(node.getExpression(), unused);
            if (result == null) {
                result = new LinkedHashSet<>();
            }
            if (node.getExpression() instanceof IdentifierTree identifierTree) {
                String owner = identifierTree.getName().toString();
                if ("this".equals(owner) || "super".equals(owner)) {
                    result.add(node.getIdentifier().toString());
                }
            }
            return result;
        }
    }

    private static final class TypeReferenceScanner extends TreeScanner<Set<String>, Void> {

        @Override
        public Set<String> reduce(Set<String> left, Set<String> right) {
            if (left == null || left.isEmpty()) {
                return right == null ? new LinkedHashSet<>() : right;
            }
            if (right != null) {
                left.addAll(right);
            }
            return left;
        }

        @Override
        public Set<String> visitVariable(VariableTree node, Void unused) {
            Set<String> result = new LinkedHashSet<>(extractTypeNames(node.getType()));
            Set<String> next = super.visitVariable(node, unused);
            if (next != null) {
                result.addAll(next);
            }
            return result;
        }

        @Override
        public Set<String> visitNewClass(NewClassTree node, Void unused) {
            Set<String> result = new LinkedHashSet<>(extractTypeNames(node.getIdentifier()));
            Set<String> next = super.visitNewClass(node, unused);
            if (next != null) {
                result.addAll(next);
            }
            return result;
        }

        @Override
        public Set<String> visitTypeCast(TypeCastTree node, Void unused) {
            Set<String> result = new LinkedHashSet<>(extractTypeNames(node.getType()));
            Set<String> next = super.visitTypeCast(node, unused);
            if (next != null) {
                result.addAll(next);
            }
            return result;
        }

        @Override
        public Set<String> visitInstanceOf(InstanceOfTree node, Void unused) {
            Set<String> result = new LinkedHashSet<>(extractTypeNames(node.getType()));
            Set<String> next = super.visitInstanceOf(node, unused);
            if (next != null) {
                result.addAll(next);
            }
            return result;
        }
    }

    private static final class InvocationScanner extends TreeScanner<Set<String>, Void> {

        @Override
        public Set<String> reduce(Set<String> left, Set<String> right) {
            if (left == null || left.isEmpty()) {
                return right == null ? new LinkedHashSet<>() : right;
            }
            if (right != null) {
                left.addAll(right);
            }
            return left;
        }

        @Override
        public Set<String> visitMethodInvocation(MethodInvocationTree node, Void unused) {
            Set<String> result = new LinkedHashSet<>();
            Tree select = node.getMethodSelect();
            if (select instanceof IdentifierTree identifierTree) {
                String methodName = identifierTree.getName().toString();
                if (!"super".equals(methodName) && !"this".equals(methodName)) {
                    result.add(methodName);
                }
            } else if (select instanceof MemberSelectTree memberSelectTree) {
                result.add(memberSelectTree.getIdentifier().toString());
            }
            Set<String> next = super.visitMethodInvocation(node, unused);
            if (next != null) {
                result.addAll(next);
            }
            return result;
        }
    }

    private static Set<String> defaultIfNull(Set<String> values) {
        return values == null ? Set.of() : values;
    }

    private static Set<String> extractTypeNames(Tree tree) {
        Set<String> result = new LinkedHashSet<>();
        if (tree == null) {
            return result;
        }
        if (tree instanceof PrimitiveTypeTree) {
            return result;
        }
        if (tree instanceof IdentifierTree identifierTree) {
            result.add(identifierTree.getName().toString());
            return result;
        }
        if (tree instanceof MemberSelectTree memberSelectTree) {
            result.add(memberSelectTree.getIdentifier().toString());
            return result;
        }
        if (tree instanceof ParameterizedTypeTree parameterizedTypeTree) {
            result.addAll(extractTypeNames(parameterizedTypeTree.getType()));
            for (Tree argument : parameterizedTypeTree.getTypeArguments()) {
                result.addAll(extractTypeNames(argument));
            }
            return result;
        }
        if (tree instanceof ArrayTypeTree arrayTypeTree) {
            result.addAll(extractTypeNames(arrayTypeTree.getType()));
            return result;
        }
        if (tree instanceof UnionTypeTree unionTypeTree) {
            for (Tree current : unionTypeTree.getTypeAlternatives()) {
                result.addAll(extractTypeNames(current));
            }
            return result;
        }
        if (tree instanceof IntersectionTypeTree intersectionTypeTree) {
            for (Tree current : intersectionTypeTree.getBounds()) {
                result.addAll(extractTypeNames(current));
            }
            return result;
        }
        return result;
    }

    private static String simpleTypeName(Tree tree) {
        Set<String> names = extractTypeNames(tree);
        if (names.isEmpty()) {
            return "";
        }
        return names.iterator().next();
    }

    private static String normalizeType(Tree.Kind kind) {
        if (kind == Tree.Kind.INTERFACE) {
            return "INTERFACE";
        }
        if (kind == Tree.Kind.ENUM) {
            return "ENUM";
        }
        if (kind == Tree.Kind.RECORD) {
            return "RECORD";
        }
        return "CLASS";
    }

    record ParsedSource(String fileName, List<TypeInfo> types) {
    }

    record TypeInfo(
            String name,
            String kind,
            String extendsName,
            List<String> implementsNames,
            int startLine,
            int endLine,
            List<FieldInfo> fields,
            List<MethodInfo> methods,
            Set<String> referencedTypes
    ) {
    }

    record FieldInfo(String name) {
    }

    record MethodInfo(
            String name,
            int startLine,
            int endLine,
            boolean executable,
            int cyclomaticComplexity,
            Set<String> fieldReferences,
            Set<String> invokedMethods
    ) {
    }

    private static final class StringJavaFileObject extends SimpleJavaFileObject {

        private final String source;

        private StringJavaFileObject(String fileName, String source) {
            super(URI.create("string:///" + normalizeFileName(fileName)), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        private static String normalizeFileName(String fileName) {
            if (fileName == null || fileName.isBlank()) {
                return "Sample.java";
            }
            return fileName.replace('\\', '/').replaceAll("[^A-Za-z0-9_./-]", "_");
        }
    }
}
