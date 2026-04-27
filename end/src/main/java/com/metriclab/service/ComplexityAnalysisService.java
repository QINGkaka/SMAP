package com.metriclab.service;

import com.metriclab.model.dto.AnalysisScopeRequest;
import com.metriclab.model.dto.ComplexityAnalysisResult;
import com.metriclab.model.dto.ComplexityFileMetric;
import com.metriclab.model.dto.ComplexityReportResult;
import com.metriclab.model.dto.ComplexitySummary;
import com.metriclab.model.dto.MethodComplexityMetric;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ComplexityAnalysisService {

    private static final Pattern DECISION_KEYWORDS = Pattern.compile("\\b(if|for|while|case|catch)\\b");
    private static final Set<String> NON_METHOD_KEYWORDS = Set.of(
            "if", "for", "while", "switch", "catch", "return", "throw", "new", "else", "do", "case", "try",
            "class", "interface", "enum", "record"
    );

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public ComplexityAnalysisService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized ComplexityAnalysisResult analyzeProject(String projectId) throws IOException {
        return analyzeProject(projectId, null);
    }

    public synchronized ComplexityAnalysisResult analyzeProject(String projectId, AnalysisScopeRequest request) throws IOException {
        List<UploadedFileInfo> uploadedFiles = resolveTargetFiles(projectId, request);
        List<String> analyzedFileIds = resolvedFileIds(uploadedFiles, request);
        List<MethodComplexityMetric> methodMetrics = new ArrayList<>();
        List<ComplexityFileMetric> fileMetrics = new ArrayList<>();
        for (UploadedFileInfo uploadedFile : uploadedFiles) {
            Path storedPath = fileStorageService.uploadsDirectory(projectId).resolve(uploadedFile.storedName());
            if ("java".equals(uploadedFile.fileType())) {
                AnalysisUnit unit = analyzeJavaFile(storedPath, uploadedFile.originalName(), uploadedFile.originalName());
                methodMetrics.addAll(unit.methods());
                fileMetrics.add(unit.file());
            } else if ("zip".equals(uploadedFile.fileType())) {
                List<AnalysisUnit> units = analyzeZipFile(storedPath, uploadedFile.originalName());
                for (AnalysisUnit unit : units) {
                    methodMetrics.addAll(unit.methods());
                    fileMetrics.add(unit.file());
                }
            }
        }
        if (fileMetrics.isEmpty()) {
            throw new IllegalArgumentException("当前项目没有可分析的 Java 文件，请上传 .java 或包含 Java 文件的 .zip");
        }

        methodMetrics = methodMetrics.stream()
                .sorted(Comparator.comparing(MethodComplexityMetric::cyclomaticComplexity).reversed()
                        .thenComparing(MethodComplexityMetric::fileName)
                        .thenComparing(MethodComplexityMetric::startLine))
                .toList();
        fileMetrics = fileMetrics.stream()
                .sorted(Comparator.comparing(ComplexityFileMetric::fileName))
                .toList();
        ComplexitySummary summary = summarize(fileMetrics.size(), methodMetrics);
        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        ComplexityAnalysisResult result = new ComplexityAnalysisResult(taskId, projectId, summary, fileMetrics, methodMetrics, now, analyzedFileIds);
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "COMPLEXITY", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("complexity-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestComplexityResultPath(projectId), result);
        return result;
    }

    public ComplexityAnalysisResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestComplexityResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        try {
            return fileStorageService.readJson(latestPath, ComplexityAnalysisResult.class);
        } catch (IOException exception) {
            return null;
        }
    }

    public ComplexityReportResult exportMarkdownReport(String projectId) throws IOException {
        ComplexityAnalysisResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有圈复杂度分析结果，请先执行控制流复杂度分析");
        }
        String content = buildMarkdownReport(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("complexity-report.md");
        fileStorageService.writeJson(fileStorageService.reportsDirectory(projectId).resolve("complexity-report-meta.json"),
                new ReportMeta(projectId, result.taskId(), reportPath.toString(), OffsetDateTime.now()));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new ComplexityReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private AnalysisUnit analyzeJavaFile(Path filePath, String fileName, String sourceUploadName) throws IOException {
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

    private List<AnalysisUnit> analyzeZipFile(Path zipPath, String sourceUploadName) throws IOException {
        List<AnalysisUnit> units = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !ZipEntryFilter.isAnalyzableJava(entry.getName())) {
                    continue;
                }
                String source = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                units.add(analyzeJavaSource(entry.getName(), sourceUploadName, source));
            }
        }
        return units;
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

    private AnalysisUnit analyzeJavaSource(String fileName, String sourceUploadName, String source) {
        JavaAstSupport.ParsedSource parsedSource = JavaAstSupport.parse(fileName, source);
        if (parsedSource != null) {
            AnalysisUnit astUnit = analyzeWithAst(fileName, sourceUploadName, parsedSource);
            if (astUnit != null) {
                return astUnit;
            }
        }
        List<String> lines = List.of(source.split("\\R", -1));
        return analyzeJavaLines(fileName, sourceUploadName, lines);
    }

    private AnalysisUnit analyzeWithAst(String fileName, String sourceUploadName, JavaAstSupport.ParsedSource parsedSource) {
        int declaredMethodCount = parsedSource.types().stream()
                .mapToInt(type -> type.methods().size())
                .sum();
        List<MethodComplexityMetric> methodMetrics = parsedSource.types().stream()
                .flatMap(type -> type.methods().stream())
                .filter(JavaAstSupport.MethodInfo::executable)
                .map(method -> new MethodComplexityMetric(
                        method.name(),
                        fileName,
                        sourceUploadName,
                        method.startLine(),
                        method.endLine(),
                        method.cyclomaticComplexity(),
                        riskLevel(method.cyclomaticComplexity())
                ))
                .toList();
        if (declaredMethodCount == 0 && methodMetrics.isEmpty()) {
            return null;
        }
        String status = methodMetrics.isEmpty()
                ? "已基于 AST 扫描，未识别到带方法体的可执行方法，通常是接口或抽象声明"
                : "已基于 AST 完成控制流复杂度分析";
        ComplexityFileMetric fileMetric = new ComplexityFileMetric(
                fileName,
                sourceUploadName,
                declaredMethodCount,
                methodMetrics.size(),
                status
        );
        return new AnalysisUnit(fileMetric, methodMetrics);
    }

    private AnalysisUnit analyzeJavaLines(String fileName, String sourceUploadName, List<String> lines) {
        List<MethodBlock> methods = extractMethods(lines);
        List<MethodComplexityMetric> methodMetrics = methods.stream()
                .map(method -> {
                    int complexity = countCyclomaticComplexity(method.lines());
                    return new MethodComplexityMetric(
                            method.name(),
                            fileName,
                            sourceUploadName,
                            method.startLine(),
                            method.endLine(),
                            complexity,
                            riskLevel(complexity)
                    );
                })
                .toList();
        int declaredMethodCount = countDeclaredMethods(lines);
        String status = methods.isEmpty()
                ? "未识别到带方法体的可执行方法，通常是接口或抽象声明"
                : "已完成控制流复杂度分析";
        ComplexityFileMetric fileMetric = new ComplexityFileMetric(
                fileName,
                sourceUploadName,
                declaredMethodCount,
                methods.size(),
                status
        );
        return new AnalysisUnit(fileMetric, methodMetrics);
    }

    private List<MethodBlock> extractMethods(List<String> lines) {
        List<MethodBlock> methods = new ArrayList<>();
        boolean inMethod = false;
        boolean collectingSignature = false;
        int braceDepth = 0;
        int startLine = 0;
        String methodName = "";
        List<String> methodLines = new ArrayList<>();
        List<String> pendingLines = new ArrayList<>();
        StringBuilder signature = new StringBuilder();

        for (int index = 0; index < lines.size(); index++) {
            String rawLine = lines.get(index);
            String codeLine = stripLineComment(rawLine).trim();

            if (inMethod) {
                methodLines.add(rawLine);
                braceDepth += countChar(codeLine, '{') - countChar(codeLine, '}');
                if (braceDepth <= 0) {
                    methods.add(new MethodBlock(methodName, startLine, index + 1, List.copyOf(methodLines)));
                    inMethod = false;
                    methodLines = new ArrayList<>();
                    methodName = "";
                }
                continue;
            }

            if (collectingSignature) {
                pendingLines.add(rawLine);
                signature.append(' ').append(codeLine);
                if (codeLine.contains(";")) {
                    collectingSignature = false;
                    pendingLines = new ArrayList<>();
                    signature = new StringBuilder();
                    continue;
                }
                if (codeLine.contains("{")) {
                    String signatureText = signature.toString();
                    if (isMethodSignature(signatureText)) {
                        inMethod = true;
                        collectingSignature = false;
                        methodName = extractMethodName(signatureText);
                        methodLines = new ArrayList<>(pendingLines);
                        braceDepth = countChar(signatureText, '{') - countChar(signatureText, '}');
                        if (braceDepth <= 0) {
                            methods.add(new MethodBlock(methodName, startLine, index + 1, List.copyOf(methodLines)));
                            inMethod = false;
                            methodLines = new ArrayList<>();
                            methodName = "";
                        }
                    } else {
                        collectingSignature = false;
                    }
                    pendingLines = new ArrayList<>();
                    signature = new StringBuilder();
                }
                continue;
            }

            if (couldStartMethodSignature(codeLine)) {
                collectingSignature = true;
                startLine = index + 1;
                pendingLines.add(rawLine);
                signature.append(codeLine);
                if (codeLine.contains("{")) {
                    String signatureText = signature.toString();
                    if (isMethodSignature(signatureText)) {
                        inMethod = true;
                        collectingSignature = false;
                        methodName = extractMethodName(signatureText);
                        methodLines = new ArrayList<>(pendingLines);
                        braceDepth = countChar(signatureText, '{') - countChar(signatureText, '}');
                        if (braceDepth <= 0) {
                            methods.add(new MethodBlock(methodName, startLine, index + 1, List.copyOf(methodLines)));
                            inMethod = false;
                            methodLines = new ArrayList<>();
                            methodName = "";
                        }
                    } else {
                        collectingSignature = false;
                    }
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

    private boolean isMethodSignature(String signature) {
        String normalized = signature.replaceAll("\\s+", " ").trim();
        int leftParen = normalized.indexOf('(');
        int rightParen = normalized.indexOf(')', leftParen + 1);
        if (leftParen <= 0 || rightParen < leftParen || !normalized.contains("{")) {
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
        String name = extractMethodName(normalized);
        return !name.isBlank() && !NON_METHOD_KEYWORDS.contains(name.toLowerCase());
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
                String signatureText = signature.toString();
                if (looksLikeMethodDeclaration(signatureText)) {
                    count++;
                }
                signature = new StringBuilder();
            }
        }
        return count;
    }

    private boolean looksLikeMethodDeclaration(String signature) {
        String normalized = signature.replaceAll("\\s+", " ").trim();
        int leftParen = normalized.indexOf('(');
        int rightParen = normalized.indexOf(')', leftParen + 1);
        if (leftParen <= 0 || rightParen < leftParen) {
            return false;
        }
        String beforeParen = normalized.substring(0, leftParen).trim();
        String lower = beforeParen.toLowerCase();
        if (lower.contains(" class ") || lower.startsWith("class ")
                || lower.contains(" interface ") || lower.startsWith("interface ")
                || lower.contains(" enum ") || lower.startsWith("enum ")
                || lower.contains(" record ") || lower.startsWith("record ")
                || lower.contains("=")) {
            return false;
        }
        String name = extractMethodName(normalized);
        return !name.isBlank() && !NON_METHOD_KEYWORDS.contains(name.toLowerCase());
    }

    private String extractMethodName(String signature) {
        int leftParen = signature.indexOf('(');
        if (leftParen < 0) {
            return "";
        }
        String beforeParen = signature.substring(0, leftParen).trim();
        String[] tokens = beforeParen.split("\\s+");
        String name = tokens.length == 0 ? "" : tokens[tokens.length - 1];
        return name.replaceAll("[^A-Za-z0-9_$]", "");
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

    private String removeStringLiterals(String line) {
        return line.replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"")
                .replaceAll("'(?:\\\\.|[^'\\\\])*'", "''");
    }

    private String stripLineComment(String line) {
        int index = line.indexOf("//");
        if (index < 0) {
            return line;
        }
        return line.substring(0, index);
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

    private ComplexitySummary summarize(int fileCount, List<MethodComplexityMetric> methods) {
        int methodCount = methods.size();
        int totalComplexity = methods.stream().mapToInt(MethodComplexityMetric::cyclomaticComplexity).sum();
        int maxComplexity = methods.stream().mapToInt(MethodComplexityMetric::cyclomaticComplexity).max().orElse(0);
        int highRiskMethodCount = (int) methods.stream()
                .filter(method -> method.cyclomaticComplexity() > 10)
                .count();
        double averageComplexity = methodCount == 0 ? 0 : Math.round(totalComplexity * 100.0 / methodCount) / 100.0;
        return new ComplexitySummary(fileCount, methodCount, averageComplexity, maxComplexity, highRiskMethodCount);
    }

    private String riskLevel(int complexity) {
        if (complexity <= 10) {
            return "LOW";
        }
        if (complexity <= 20) {
            return "MEDIUM";
        }
        if (complexity <= 50) {
            return "HIGH";
        }
        return "EXTREME";
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-complexity-" + timestamp + "-" + suffix;
    }

    private String buildMarkdownReport(ComplexityAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 圈复杂度分析报告\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(result.projectId()).append("\n");
        builder.append("- 任务 ID：").append(result.taskId()).append("\n");
        builder.append("- 分析时间：").append(result.analyzedAt()).append("\n\n");
        builder.append("## 汇总结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| Java 文件数 | ").append(result.summary().fileCount()).append(" |\n");
        builder.append("| 方法数 | ").append(result.summary().methodCount()).append(" |\n");
        builder.append("| 平均圈复杂度 | ").append(result.summary().averageComplexity()).append(" |\n");
        builder.append("| 最高圈复杂度 | ").append(result.summary().maxComplexity()).append(" |\n");
        builder.append("| 需关注方法数 | ").append(result.summary().highRiskMethodCount()).append(" |\n\n");
        builder.append("## 文件扫描结果\n\n");
        builder.append("| 文件 | 来源 | 声明方法数 | 可分析方法体数 | 状态 |\n");
        builder.append("| --- | --- | ---: | ---: | --- |\n");
        List<ComplexityFileMetric> files = result.files() == null ? List.of() : result.files();
        for (ComplexityFileMetric file : files) {
            builder.append("| ")
                    .append(file.fileName()).append(" | ")
                    .append(file.sourceUploadName()).append(" | ")
                    .append(file.declaredMethodCount()).append(" | ")
                    .append(file.executableMethodCount()).append(" | ")
                    .append(file.status()).append(" |\n");
        }
        builder.append("\n");
        builder.append("## 方法明细\n\n");
        builder.append("| 方法 | 文件 | 来源 | 行号 | 圈复杂度 | 风险等级 |\n");
        builder.append("| --- | --- | --- | ---: | ---: | --- |\n");
        for (MethodComplexityMetric method : result.methods()) {
            builder.append("| ")
                    .append(method.methodName()).append(" | ")
                    .append(method.fileName()).append(" | ")
                    .append(method.sourceUploadName()).append(" | ")
                    .append(method.startLine()).append("-").append(method.endLine()).append(" | ")
                    .append(method.cyclomaticComplexity()).append(" | ")
                    .append(method.riskLevel()).append(" |\n");
        }
        return builder.toString();
    }

    private record MethodBlock(String name, int startLine, int endLine, List<String> lines) {
    }

    private record AnalysisUnit(ComplexityFileMetric file, List<MethodComplexityMetric> methods) {
    }

    private record SanitizedLine(String code, boolean inBlockComment) {
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private record ReportMeta(String projectId, String taskId, String reportPath, OffsetDateTime createdAt) {
    }
}
