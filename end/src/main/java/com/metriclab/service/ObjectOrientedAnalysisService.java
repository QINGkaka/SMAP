package com.metriclab.service;

import com.metriclab.model.dto.AnalysisScopeRequest;
import com.metriclab.model.dto.ClassMetric;
import com.metriclab.model.dto.ObjectOrientedAnalysisResult;
import com.metriclab.model.dto.ObjectOrientedReportResult;
import com.metriclab.model.dto.ObjectOrientedSummary;
import com.metriclab.model.dto.UploadedFileInfo;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ObjectOrientedAnalysisService {

    private static final Pattern TYPE_DECLARATION = Pattern.compile("\\b(class|interface|enum|record)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b");
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\bextends\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\bimplements\\s+([^\\{]+)");
    private static final Pattern TYPE_REFERENCE = Pattern.compile("\\b[A-Z][A-Za-z0-9_$]*\\b");
    private static final Pattern DECISION_KEYWORDS = Pattern.compile("\\b(if|for|while|case|catch)\\b");
    private static final Set<String> NON_METHOD_KEYWORDS = Set.of(
            "if", "for", "while", "switch", "catch", "return", "throw", "new", "else", "do", "case", "try",
            "class", "interface", "enum", "record"
    );
    private static final Set<String> COMMON_TYPES = Set.of(
            "String", "Integer", "Long", "Double", "Float", "Boolean", "Byte", "Short", "Character", "Object",
            "List", "ArrayList", "Map", "HashMap", "Set", "HashSet", "Optional", "Date", "LocalDate", "LocalDateTime",
            "Override", "Autowired", "Service", "Repository", "Component", "Data"
    );

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public ObjectOrientedAnalysisService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized ObjectOrientedAnalysisResult analyzeProject(String projectId) throws IOException {
        return analyzeProject(projectId, null);
    }

    public synchronized ObjectOrientedAnalysisResult analyzeProject(String projectId, AnalysisScopeRequest request) throws IOException {
        List<UploadedFileInfo> uploadedFiles = resolveTargetFiles(projectId, request);
        List<String> analyzedFileIds = resolvedFileIds(uploadedFiles, request);
        List<RawClassMetric> rawMetrics = new ArrayList<>();
        for (UploadedFileInfo uploadedFile : uploadedFiles) {
            Path storedPath = fileStorageService.uploadsDirectory(projectId).resolve(uploadedFile.storedName());
            if ("java".equals(uploadedFile.fileType())) {
                rawMetrics.addAll(analyzeJavaFile(storedPath, uploadedFile.originalName(), uploadedFile.originalName()));
            } else if ("zip".equals(uploadedFile.fileType())) {
                rawMetrics.addAll(analyzeZipFile(storedPath, uploadedFile.originalName()));
            }
        }
        if (rawMetrics.isEmpty()) {
            throw new IllegalArgumentException("当前项目没有识别到 Java 类或接口，请上传 .java 或包含 Java 文件的 .zip");
        }

        Map<String, RawClassMetric> byName = new HashMap<>();
        Map<String, Integer> nocByParent = new HashMap<>();
        for (RawClassMetric metric : rawMetrics) {
            byName.put(metric.className(), metric);
            if (!metric.extendsName().isBlank()) {
                nocByParent.merge(metric.extendsName(), 1, Integer::sum);
            }
        }

        List<ClassMetric> classMetrics = rawMetrics.stream()
                .map(metric -> {
                    int dit = computeDit(metric, byName);
                    int noc = nocByParent.getOrDefault(metric.className(), 0);
                    return new ClassMetric(
                            metric.className(),
                            metric.fileName(),
                            metric.sourceUploadName(),
                            metric.type(),
                            metric.cbo(),
                            metric.rfc(),
                            dit,
                            noc,
                            metric.fieldCount(),
                            metric.methodCount(),
                            metric.classSize(),
                            metric.wmc(),
                            metric.lcom(),
                            riskLevel(metric.cbo(), metric.rfc(), dit, metric.wmc(), metric.lcom())
                    );
                })
                .sorted(Comparator.comparing(ClassMetric::riskLevel).reversed()
                        .thenComparing(ClassMetric::className))
                .toList();

        ObjectOrientedSummary summary = summarize(classMetrics);
        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        ObjectOrientedAnalysisResult result = new ObjectOrientedAnalysisResult(taskId, projectId, summary, classMetrics, now, analyzedFileIds);
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "OBJECT_ORIENTED", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("object-oriented-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestObjectOrientedResultPath(projectId), result);
        return result;
    }

    public ObjectOrientedAnalysisResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestObjectOrientedResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        try {
            return fileStorageService.readJson(latestPath, ObjectOrientedAnalysisResult.class);
        } catch (IOException exception) {
            return null;
        }
    }

    public ObjectOrientedReportResult exportMarkdownReport(String projectId) throws IOException {
        ObjectOrientedAnalysisResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有面向对象度量结果，请先执行 CK/LK 分析");
        }
        String content = buildMarkdownReport(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("object-oriented-report.md");
        fileStorageService.writeJson(fileStorageService.reportsDirectory(projectId).resolve("object-oriented-report-meta.json"),
                new ReportMeta(projectId, result.taskId(), reportPath.toString(), OffsetDateTime.now()));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new ObjectOrientedReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private List<RawClassMetric> analyzeJavaFile(Path filePath, String fileName, String sourceUploadName) throws IOException {
        String source = Files.readString(filePath, StandardCharsets.UTF_8);
        return analyzeJavaSource(fileName, sourceUploadName, source);
    }

    private List<UploadedFileInfo> resolveTargetFiles(String projectId, AnalysisScopeRequest request) throws IOException {
        List<UploadedFileInfo> uploadedFiles = uploadService.listFiles(projectId);
        List<String> fileIds = request == null ? List.of() : request.fileIds();
        if (fileIds == null || fileIds.isEmpty()) {
            return uploadedFiles;
        }
        Set<String> selectedIds = Set.copyOf(fileIds);
        List<UploadedFileInfo> selectedFiles = uploadedFiles.stream()
                .filter(file -> selectedIds.contains(file.id()))
                .toList();
        if (selectedFiles.size() != selectedIds.size()) {
            throw new IllegalArgumentException("所选文件中存在无效文件，请刷新项目文件列表后重试");
        }
        List<UploadedFileInfo> analyzableFiles = selectedFiles.stream()
                .filter(file -> "java".equals(file.fileType()) || "zip".equals(file.fileType()))
                .toList();
        if (analyzableFiles.isEmpty()) {
            throw new IllegalArgumentException("当前选择中没有可分析的 Java 文件，请选择 .java 或 .zip 文件");
        }
        return analyzableFiles;
    }

    private List<RawClassMetric> analyzeZipFile(Path zipPath, String sourceUploadName) throws IOException {
        List<RawClassMetric> metrics = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !ZipEntryFilter.isAnalyzableJava(entry.getName())) {
                    continue;
                }
                String source = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                metrics.addAll(analyzeJavaSource(entry.getName(), sourceUploadName, source));
            }
        }
        return metrics;
    }

    private List<String> resolvedFileIds(List<UploadedFileInfo> uploadedFiles, AnalysisScopeRequest request) {
        List<String> fileIds = request == null ? List.of() : request.fileIds();
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return uploadedFiles.stream()
                .map(UploadedFileInfo::id)
                .toList();
    }

    private List<RawClassMetric> analyzeJavaSource(String fileName, String sourceUploadName, String source) {
        JavaAstSupport.ParsedSource parsedSource = JavaAstSupport.parse(fileName, source);
        if (parsedSource != null) {
            List<RawClassMetric> astMetrics = analyzeWithAst(fileName, sourceUploadName, parsedSource);
            if (!astMetrics.isEmpty()) {
                return astMetrics;
            }
        }
        List<String> lines = List.of(source.split("\\R", -1));
        return analyzeJavaLines(fileName, sourceUploadName, lines);
    }

    private List<RawClassMetric> analyzeWithAst(String fileName, String sourceUploadName, JavaAstSupport.ParsedSource parsedSource) {
        return parsedSource.types().stream()
                .map(type -> {
                    List<String> fieldNames = type.fields().stream()
                            .map(JavaAstSupport.FieldInfo::name)
                            .toList();
                    int methodCount = type.methods().size();
                    List<JavaAstSupport.MethodInfo> executableMethods = type.methods().stream()
                            .filter(JavaAstSupport.MethodInfo::executable)
                            .toList();
                    int wmc = executableMethods.stream()
                            .mapToInt(JavaAstSupport.MethodInfo::cyclomaticComplexity)
                            .sum();
                    if (wmc == 0) {
                        wmc = methodCount;
                    }
                    int lcom = calculateLcomFromAst(fieldNames, type.methods());
                    int cbo = calculateCboFromAst(type, fieldNames);
                    int rfc = calculateRfcFromAst(type.methods());
                    int classSize = Math.max(1, type.endLine() - type.startLine() + 1);
                    return new RawClassMetric(
                            type.name(),
                            fileName,
                            sourceUploadName,
                            type.kind(),
                            type.extendsName(),
                            cbo,
                            rfc,
                            fieldNames.size(),
                            methodCount,
                            classSize,
                            wmc,
                            lcom
                    );
                })
                .toList();
    }

    private List<RawClassMetric> analyzeJavaLines(String fileName, String sourceUploadName, List<String> lines) {
        return extractTypeBlocks(lines).stream()
                .map(block -> analyzeTypeBlock(fileName, sourceUploadName, block))
                .toList();
    }

    private RawClassMetric analyzeTypeBlock(String fileName, String sourceUploadName, TypeBlock block) {
        List<String> lines = block.lines();
        List<String> fieldNames = extractFieldNames(lines);
        List<MethodBlock> executableMethods = extractExecutableMethods(lines);
        int declaredMethodCount = countDeclaredMethods(lines);
        int methodCount = Math.max(declaredMethodCount, executableMethods.size());
        int wmc = executableMethods.stream()
                .mapToInt(method -> countCyclomaticComplexity(method.lines()))
                .sum();
        if (wmc == 0) {
            wmc = methodCount;
        }
        int lcom = calculateLcom(fieldNames, executableMethods);
        int cbo = calculateCbo(block, fieldNames);
        int rfc = calculateRfc(lines);
        return new RawClassMetric(
                block.name(),
                fileName,
                sourceUploadName,
                block.type(),
                block.extendsName(),
                cbo,
                rfc,
                fieldNames.size(),
                methodCount,
                lines.size(),
                wmc,
                lcom
        );
    }

    private List<TypeBlock> extractTypeBlocks(List<String> lines) {
        List<TypeBlock> blocks = new ArrayList<>();
        boolean collecting = false;
        boolean inType = false;
        int startLine = 0;
        int braceDepth = 0;
        StringBuilder signature = new StringBuilder();
        List<String> typeLines = new ArrayList<>();
        String type = "";
        String name = "";

        for (int index = 0; index < lines.size(); index++) {
            String rawLine = lines.get(index);
            String codeLine = stripLineComment(rawLine).trim();
            if (inType) {
                typeLines.add(rawLine);
                braceDepth += countChar(codeLine, '{') - countChar(codeLine, '}');
                if (braceDepth <= 0) {
                    blocks.add(createTypeBlock(type, name, signature.toString(), List.copyOf(typeLines), startLine, index + 1));
                    inType = false;
                    collecting = false;
                    typeLines = new ArrayList<>();
                    signature = new StringBuilder();
                }
                continue;
            }

            if (!collecting) {
                Matcher matcher = TYPE_DECLARATION.matcher(codeLine);
                if (!matcher.find()) {
                    continue;
                }
                collecting = true;
                startLine = index + 1;
                type = matcher.group(1).toUpperCase();
                name = matcher.group(2);
            }

            if (collecting) {
                signature.append(' ').append(codeLine);
                typeLines.add(rawLine);
                if (codeLine.contains("{")) {
                    inType = true;
                    braceDepth = countChar(codeLine, '{') - countChar(codeLine, '}');
                    if (braceDepth <= 0) {
                        blocks.add(createTypeBlock(type, name, signature.toString(), List.copyOf(typeLines), startLine, index + 1));
                        inType = false;
                        collecting = false;
                        typeLines = new ArrayList<>();
                        signature = new StringBuilder();
                    }
                }
            }
        }
        return blocks;
    }

    private TypeBlock createTypeBlock(String type, String name, String signature, List<String> lines, int startLine, int endLine) {
        String extendsName = "";
        Matcher extendsMatcher = EXTENDS_PATTERN.matcher(signature);
        if (extendsMatcher.find()) {
            extendsName = extendsMatcher.group(1);
        }
        List<String> implementsNames = new ArrayList<>();
        Matcher implementsMatcher = IMPLEMENTS_PATTERN.matcher(signature);
        if (implementsMatcher.find()) {
            String[] names = implementsMatcher.group(1).split(",");
            for (String current : names) {
                String normalized = current.replaceAll("<.*>", "").trim();
                if (!normalized.isBlank()) {
                    implementsNames.add(normalized);
                }
            }
        }
        return new TypeBlock(type, name, extendsName, implementsNames, lines, startLine, endLine);
    }

    private List<String> extractFieldNames(List<String> lines) {
        List<String> fields = new ArrayList<>();
        int depth = 0;
        for (String line : lines) {
            String code = stripLineComment(line).trim();
            if (code.isBlank() || code.startsWith("@")) {
                depth += countChar(code, '{') - countChar(code, '}');
                continue;
            }
            if (depth == 1 && code.endsWith(";") && !code.contains("(")
                    && !code.startsWith("return ") && !code.startsWith("throw ")
                    && !code.startsWith("package ") && !code.startsWith("import ")) {
                fields.addAll(parseFieldNames(code));
            }
            depth += countChar(code, '{') - countChar(code, '}');
        }
        return fields;
    }

    private List<String> parseFieldNames(String line) {
        String cleaned = line.replace(";", "").replaceAll("=.*?(,|$)", "$1");
        String[] declarations = cleaned.split(",");
        List<String> names = new ArrayList<>();
        for (String declaration : declarations) {
            String[] tokens = declaration.trim().split("\\s+");
            if (tokens.length == 0) {
                continue;
            }
            String name = tokens[tokens.length - 1].replaceAll("[^A-Za-z0-9_$]", "");
            if (!name.isBlank() && !Set.of("public", "private", "protected", "static", "final").contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private int countDeclaredMethods(List<String> lines) {
        int count = 0;
        StringBuilder signature = new StringBuilder();
        for (String line : lines) {
            String codeLine = stripLineComment(line).trim();
            if (codeLine.isBlank() || codeLine.startsWith("@")) {
                continue;
            }
            signature.append(' ').append(codeLine);
            if (codeLine.contains(";") || codeLine.contains("{")) {
                if (looksLikeMethodDeclaration(signature.toString())) {
                    count++;
                }
                signature = new StringBuilder();
            }
        }
        return count;
    }

    private List<MethodBlock> extractExecutableMethods(List<String> lines) {
        List<MethodBlock> methods = new ArrayList<>();
        boolean inMethod = false;
        boolean collecting = false;
        int braceDepth = 0;
        List<String> methodLines = new ArrayList<>();
        List<String> pendingLines = new ArrayList<>();
        StringBuilder signature = new StringBuilder();

        for (String line : lines) {
            String codeLine = stripLineComment(line).trim();
            if (inMethod) {
                methodLines.add(line);
                braceDepth += countChar(codeLine, '{') - countChar(codeLine, '}');
                if (braceDepth <= 0) {
                    methods.add(new MethodBlock(List.copyOf(methodLines)));
                    inMethod = false;
                    methodLines = new ArrayList<>();
                }
                continue;
            }
            if (collecting) {
                pendingLines.add(line);
                signature.append(' ').append(codeLine);
                if (codeLine.contains(";")) {
                    collecting = false;
                    pendingLines = new ArrayList<>();
                    signature = new StringBuilder();
                    continue;
                }
                if (codeLine.contains("{")) {
                    if (looksLikeMethodDeclaration(signature.toString())) {
                        inMethod = true;
                        methodLines = new ArrayList<>(pendingLines);
                        braceDepth = countChar(signature.toString(), '{') - countChar(signature.toString(), '}');
                        if (braceDepth <= 0) {
                            methods.add(new MethodBlock(List.copyOf(methodLines)));
                            inMethod = false;
                            methodLines = new ArrayList<>();
                        }
                    }
                    collecting = false;
                    pendingLines = new ArrayList<>();
                    signature = new StringBuilder();
                }
                continue;
            }
            if (couldStartMethodSignature(codeLine)) {
                collecting = true;
                pendingLines.add(line);
                signature.append(codeLine);
                if (codeLine.contains("{")) {
                    if (looksLikeMethodDeclaration(signature.toString())) {
                        inMethod = true;
                        methodLines = new ArrayList<>(pendingLines);
                        braceDepth = countChar(signature.toString(), '{') - countChar(signature.toString(), '}');
                        if (braceDepth <= 0) {
                            methods.add(new MethodBlock(List.copyOf(methodLines)));
                            inMethod = false;
                            methodLines = new ArrayList<>();
                        }
                    }
                    collecting = false;
                    pendingLines = new ArrayList<>();
                    signature = new StringBuilder();
                }
            }
        }
        return methods;
    }

    private boolean couldStartMethodSignature(String line) {
        if (line.isBlank() || line.startsWith("@") || !line.contains("(")) {
            return false;
        }
        String lower = line.toLowerCase();
        if (lower.contains("->") || lower.startsWith("package ") || lower.startsWith("import ")) {
            return false;
        }
        String firstToken = lower.split("\\s+")[0];
        return !NON_METHOD_KEYWORDS.contains(firstToken);
    }

    private boolean looksLikeMethodDeclaration(String signature) {
        String normalized = signature.replaceAll("\\s+", " ").trim();
        int leftParen = normalized.indexOf('(');
        int rightParen = normalized.indexOf(')', leftParen + 1);
        if (leftParen <= 0 || rightParen < leftParen) {
            return false;
        }
        String beforeParen = normalized.substring(0, leftParen).trim();
        String[] tokens = beforeParen.split("\\s+");
        String lower = beforeParen.toLowerCase();
        if (tokens.length < 2
                || beforeParen.contains(".")
                || lower.contains(" class ") || lower.startsWith("class ")
                || lower.contains(" interface ") || lower.startsWith("interface ")
                || lower.contains(" enum ") || lower.startsWith("enum ")
                || lower.contains(" record ") || lower.startsWith("record ")
                || lower.contains("=")) {
            return false;
        }
        String name = tokens[tokens.length - 1].replaceAll("[^A-Za-z0-9_$]", "");
        return !name.isBlank() && !NON_METHOD_KEYWORDS.contains(name.toLowerCase());
    }

    private int countCyclomaticComplexity(List<String> lines) {
        int complexity = 1;
        boolean inBlockComment = false;
        for (String line : lines) {
            SanitizedLine sanitizedLine = sanitizeCodeLine(line, inBlockComment);
            inBlockComment = sanitizedLine.inBlockComment();
            String code = sanitizedLine.code();
            Matcher matcher = DECISION_KEYWORDS.matcher(code);
            while (matcher.find()) {
                complexity++;
            }
            complexity += countOccurrences(code, "&&");
            complexity += countOccurrences(code, "||");
            complexity += countChar(code, '?');
        }
        return complexity;
    }

    private int calculateLcom(List<String> fields, List<MethodBlock> methods) {
        if (methods.size() <= 1) {
            return 0;
        }
        List<Set<String>> references = new ArrayList<>();
        for (MethodBlock method : methods) {
            String body = String.join("\n", method.lines());
            Set<String> methodFields = new HashSet<>();
            for (String field : fields) {
                if (body.matches("(?s).*\\b" + Pattern.quote(field) + "\\b.*")) {
                    methodFields.add(field);
                }
            }
            references.add(methodFields);
        }
        return computeLcomFromReferences(references);
    }

    private int calculateLcomFromAst(List<String> fields, List<JavaAstSupport.MethodInfo> methods) {
        if (methods.size() <= 1) {
            return 0;
        }
        List<Set<String>> references = new ArrayList<>();
        for (JavaAstSupport.MethodInfo method : methods) {
            Set<String> methodFields = new HashSet<>();
            for (String reference : method.fieldReferences()) {
                if (fields.contains(reference)) {
                    methodFields.add(reference);
                }
            }
            references.add(methodFields);
        }
        return computeLcomFromReferences(references);
    }

    private int computeLcomFromReferences(List<Set<String>> references) {
        int p = 0;
        int q = 0;
        for (int i = 0; i < references.size(); i++) {
            for (int j = i + 1; j < references.size(); j++) {
                Set<String> intersection = new HashSet<>(references.get(i));
                intersection.retainAll(references.get(j));
                if (intersection.isEmpty()) {
                    p++;
                } else {
                    q++;
                }
            }
        }
        return Math.max(p - q, 0);
    }

    private int calculateLcomFromAst(List<String> fields, List<JavaAstSupport.MethodInfo> methods, int methodCount) {
        if (methodCount <= 1 || fields.isEmpty() || methods.isEmpty()) {
            return 0;
        }
        int methodsTouchingFields = 0;
        for (JavaAstSupport.MethodInfo method : methods) {
            boolean touchesField = method.fieldReferences().stream().anyMatch(fields::contains);
            if (touchesField) {
                methodsTouchingFields++;
            }
        }
        return Math.max(0, methodCount - methodsTouchingFields);
    }

    private int calculateCbo(TypeBlock block, List<String> fields) {
        Set<String> references = new HashSet<>();
        for (String line : block.lines()) {
            Matcher matcher = TYPE_REFERENCE.matcher(removeStringLiterals(stripLineComment(line)));
            while (matcher.find()) {
                String reference = matcher.group();
                if (!reference.equals(block.name()) && !COMMON_TYPES.contains(reference)) {
                    references.add(reference);
                }
            }
        }
        references.removeAll(fields);
        references.remove(block.extendsName());
        references.removeAll(block.implementsNames());
        return references.size();
    }

    private int calculateCboFromAst(JavaAstSupport.TypeInfo type, List<String> fields) {
        Set<String> references = new HashSet<>(type.referencedTypes());
        references.remove(type.name());
        references.remove(type.extendsName());
        references.removeAll(type.implementsNames());
        references.removeAll(fields);
        references.removeIf(reference -> reference == null
                || reference.isBlank()
                || COMMON_TYPES.contains(reference)
                || Character.isLowerCase(reference.charAt(0)));
        return references.size();
    }

    private int calculateRfcFromAst(List<JavaAstSupport.MethodInfo> methods) {
        Set<String> responseSet = new HashSet<>();
        for (JavaAstSupport.MethodInfo method : methods) {
            responseSet.add(method.name());
            responseSet.addAll(method.invokedMethods());
        }
        return responseSet.size();
    }

    private int calculateRfc(List<String> lines) {
        Set<String> declaredMethods = new HashSet<>();
        Set<String> invokedMethods = new HashSet<>();
        Pattern invocationPattern = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(");
        StringBuilder signature = new StringBuilder();
        for (String line : lines) {
            String codeLine = stripLineComment(line).trim();
            if (codeLine.isBlank() || codeLine.startsWith("@")) {
                continue;
            }
            signature.append(' ').append(codeLine);
            Matcher invocationMatcher = invocationPattern.matcher(codeLine);
            while (invocationMatcher.find()) {
                String name = invocationMatcher.group(1);
                if (!NON_METHOD_KEYWORDS.contains(name.toLowerCase())) {
                    invokedMethods.add(name);
                }
            }
            if (codeLine.contains(";") || codeLine.contains("{")) {
                if (looksLikeMethodDeclaration(signature.toString())) {
                    String normalized = signature.toString().replaceAll("\\s+", " ").trim();
                    int leftParen = normalized.indexOf('(');
                    if (leftParen > 0) {
                        String beforeParen = normalized.substring(0, leftParen).trim();
                        String[] tokens = beforeParen.split("\\s+");
                        declaredMethods.add(tokens[tokens.length - 1].replaceAll("[^A-Za-z0-9_$]", ""));
                    }
                }
                signature = new StringBuilder();
            }
        }
        declaredMethods.addAll(invokedMethods);
        return declaredMethods.size();
    }

    private int computeDit(RawClassMetric metric, Map<String, RawClassMetric> byName) {
        if (metric.extendsName().isBlank()) {
            return 0;
        }
        int depth = 1;
        String parent = metric.extendsName();
        Set<String> visited = new HashSet<>();
        while (byName.containsKey(parent) && visited.add(parent)) {
            String next = byName.get(parent).extendsName();
            if (next.isBlank()) {
                break;
            }
            depth++;
            parent = next;
        }
        return depth;
    }

    private ObjectOrientedSummary summarize(List<ClassMetric> classes) {
        int fileCount = (int) classes.stream().map(ClassMetric::fileName).distinct().count();
        int classCount = (int) classes.stream()
                .filter(metric -> !"INTERFACE".equals(metric.type()))
                .count();
        int interfaceCount = (int) classes.stream()
                .filter(metric -> "INTERFACE".equals(metric.type()))
                .count();
        int methodCount = classes.stream().mapToInt(ClassMetric::noo).sum();
        int fieldCount = classes.stream().mapToInt(ClassMetric::noa).sum();
        int totalCbo = classes.stream().mapToInt(ClassMetric::cbo).sum();
        int totalRfc = classes.stream().mapToInt(ClassMetric::rfc).sum();
        int maxDit = classes.stream().mapToInt(ClassMetric::dit).max().orElse(0);
        int highRiskClassCount = (int) classes.stream()
                .filter(metric -> !"LOW".equals(metric.riskLevel()))
                .count();
        double averageCbo = classes.isEmpty() ? 0 : Math.round(totalCbo * 100.0 / classes.size()) / 100.0;
        double averageRfc = classes.isEmpty() ? 0 : Math.round(totalRfc * 100.0 / classes.size()) / 100.0;
        return new ObjectOrientedSummary(fileCount, classCount, interfaceCount, methodCount, fieldCount, averageCbo, averageRfc, maxDit, highRiskClassCount);
    }

    private String riskLevel(int cbo, int rfc, int dit, int wmc, int lcom) {
        if (cbo >= 15 || rfc >= 20 || dit >= 5 || wmc >= 40 || lcom >= 20) {
            return "HIGH";
        }
        if (cbo >= 8 || rfc >= 10 || dit >= 3 || wmc >= 20 || lcom >= 10) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private SanitizedLine sanitizeCodeLine(String line, boolean inBlockComment) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while (index < line.length()) {
            if (inBlockComment) {
                int end = line.indexOf("*/", index);
                if (end < 0) {
                    return new SanitizedLine(builder.toString(), true);
                }
                index = end + 2;
                inBlockComment = false;
                continue;
            }
            if (line.startsWith("//", index)) {
                break;
            }
            if (line.startsWith("/*", index)) {
                inBlockComment = true;
                index += 2;
                continue;
            }
            builder.append(line.charAt(index));
            index++;
        }
        return new SanitizedLine(removeStringLiterals(builder.toString()), inBlockComment);
    }

    private String stripLineComment(String line) {
        int index = line.indexOf("//");
        return index < 0 ? line : line.substring(0, index);
    }

    private String removeStringLiterals(String line) {
        return line.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"")
                .replaceAll("'(?:\\\\.|[^'\\\\])*'", "''");
    }

    private int countChar(String text, char target) {
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == target) {
                count++;
            }
        }
        return count;
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = text.indexOf(target);
        while (index >= 0) {
            count++;
            index = text.indexOf(target, index + target.length());
        }
        return count;
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-oo-" + timestamp + "-" + suffix;
    }

    private String buildMarkdownReport(ObjectOrientedAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 面向对象 CK/LK 度量报告\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(result.projectId()).append("\n");
        builder.append("- 任务 ID：").append(result.taskId()).append("\n");
        builder.append("- 分析时间：").append(result.analyzedAt()).append("\n\n");
        builder.append("## 汇总结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| Java 文件数 | ").append(result.summary().fileCount()).append(" |\n");
        builder.append("| 类数量 | ").append(result.summary().classCount()).append(" |\n");
        builder.append("| 接口数量 | ").append(result.summary().interfaceCount()).append(" |\n");
        builder.append("| 方法数量 NOO | ").append(result.summary().methodCount()).append(" |\n");
        builder.append("| 属性数量 NOA | ").append(result.summary().fieldCount()).append(" |\n");
        builder.append("| 平均 CBO | ").append(result.summary().averageCbo()).append(" |\n");
        builder.append("| 平均 RFC | ").append(result.summary().averageRfc()).append(" |\n");
        builder.append("| 最大 DIT | ").append(result.summary().maxDit()).append(" |\n");
        builder.append("| 风险类数量 | ").append(result.summary().highRiskClassCount()).append(" |\n\n");
        builder.append("## 类级明细\n\n");
        builder.append("| 类/接口 | 类型 | 文件 | CBO | RFC | DIT | NOC | NOA | NOO | CS | WMC | LCOM | 风险 |\n");
        builder.append("| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |\n");
        for (ClassMetric metric : result.classes()) {
            builder.append("| ")
                    .append(metric.className()).append(" | ")
                    .append(metric.type()).append(" | ")
                    .append(metric.fileName()).append(" | ")
                    .append(metric.cbo()).append(" | ")
                    .append(metric.rfc()).append(" | ")
                    .append(metric.dit()).append(" | ")
                    .append(metric.noc()).append(" | ")
                    .append(metric.noa()).append(" | ")
                    .append(metric.noo()).append(" | ")
                    .append(metric.cs()).append(" | ")
                    .append(metric.wmc()).append(" | ")
                    .append(metric.lcom()).append(" | ")
                    .append(metric.riskLevel()).append(" |\n");
        }
        return builder.toString();
    }

    private record TypeBlock(String type, String name, String extendsName, List<String> implementsNames,
                             List<String> lines, int startLine, int endLine) {
    }

    private record MethodBlock(List<String> lines) {
    }

    private record RawClassMetric(String className, String fileName, String sourceUploadName, String type,
                                  String extendsName, int cbo, int rfc, int fieldCount, int methodCount, int classSize,
                                  int wmc, int lcom) {
    }

    private record SanitizedLine(String code, boolean inBlockComment) {
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private record ReportMeta(String projectId, String taskId, String reportPath, OffsetDateTime createdAt) {
    }
}
