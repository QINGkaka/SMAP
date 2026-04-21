package com.metriclab.service;

import com.metriclab.model.dto.LocAnalysisResult;
import com.metriclab.model.dto.LocFileMetric;
import com.metriclab.model.dto.LocReportResult;
import com.metriclab.model.dto.LocSummary;
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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class LocAnalysisService {

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public LocAnalysisService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized LocAnalysisResult analyzeProject(String projectId) throws IOException {
        List<UploadedFileInfo> uploadedFiles = uploadService.listFiles(projectId);
        List<LocFileMetric> fileMetrics = new ArrayList<>();
        for (UploadedFileInfo uploadedFile : uploadedFiles) {
            Path storedPath = fileStorageService.uploadsDirectory(projectId).resolve(uploadedFile.storedName());
            if ("java".equals(uploadedFile.fileType())) {
                fileMetrics.add(analyzeJavaFile(storedPath, uploadedFile.originalName(), uploadedFile.originalName()));
            } else if ("zip".equals(uploadedFile.fileType())) {
                fileMetrics.addAll(analyzeZipFile(storedPath, uploadedFile.originalName()));
            }
        }
        if (fileMetrics.isEmpty()) {
            throw new IllegalArgumentException("当前项目没有可分析的 Java 文件，请先上传 .java 或包含 Java 文件的 .zip");
        }

        fileMetrics = fileMetrics.stream()
                .sorted(Comparator.comparing(LocFileMetric::fileName))
                .toList();
        LocSummary summary = summarize(fileMetrics);
        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        LocAnalysisResult result = new LocAnalysisResult(taskId, projectId, summary, fileMetrics, now);
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "LOC", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("loc-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestLocResultPath(projectId), result);
        return result;
    }

    public LocAnalysisResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestLocResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        return fileStorageService.readJson(latestPath, LocAnalysisResult.class);
    }

    public LocReportResult exportMarkdownReport(String projectId) throws IOException {
        LocAnalysisResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有 LoC 分析结果，请先执行代码行度量");
        }
        String content = buildMarkdownReport(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("loc-report.md");
        fileStorageService.writeJson(fileStorageService.reportsDirectory(projectId).resolve("loc-report-meta.json"),
                new ReportMeta(projectId, result.taskId(), reportPath.toString(), OffsetDateTime.now()));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new LocReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private LocFileMetric analyzeJavaFile(Path filePath, String fileName, String sourceUploadName) throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        return countLines(fileName, sourceUploadName, lines);
    }

    private List<LocFileMetric> analyzeZipFile(Path zipPath, String sourceUploadName) throws IOException {
        List<LocFileMetric> metrics = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !ZipEntryFilter.isAnalyzableJava(entry.getName())) {
                    continue;
                }
                List<String> lines = List.of(new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8).split("\\R", -1));
                metrics.add(countLines(entry.getName(), sourceUploadName, lines));
            }
        }
        return metrics;
    }

    private LocFileMetric countLines(String fileName, String sourceUploadName, List<String> lines) {
        int totalLines = lines.size();
        int blankLines = 0;
        int commentLines = 0;
        int sourceLines = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            LineType type = classifyLine(line, inBlockComment);
            inBlockComment = type.inBlockComment();
            if (type.blank()) {
                blankLines++;
            } else if (type.commentOnly()) {
                commentLines++;
            } else {
                sourceLines++;
            }
        }

        return new LocFileMetric(
                fileName,
                sourceUploadName,
                totalLines,
                sourceLines,
                commentLines,
                blankLines,
                ratio(commentLines, totalLines)
        );
    }

    private LineType classifyLine(String line, boolean inBlockComment) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new LineType(true, false, inBlockComment);
        }

        if (inBlockComment) {
            int end = trimmed.indexOf("*/");
            if (end < 0) {
                return new LineType(false, true, true);
            }
            String after = trimmed.substring(end + 2).trim();
            if (after.isEmpty() || after.startsWith("//")) {
                return new LineType(false, true, false);
            }
            return new LineType(false, false, false);
        }

        if (trimmed.startsWith("//")) {
            return new LineType(false, true, false);
        }

        if (trimmed.startsWith("/*")) {
            int end = trimmed.indexOf("*/", 2);
            if (end < 0) {
                return new LineType(false, true, true);
            }
            String after = trimmed.substring(end + 2).trim();
            if (after.isEmpty() || after.startsWith("//")) {
                return new LineType(false, true, false);
            }
            return new LineType(false, false, false);
        }

        int blockStart = trimmed.indexOf("/*");
        if (blockStart >= 0 && trimmed.indexOf("*/", blockStart + 2) < 0) {
            return new LineType(false, false, true);
        }
        return new LineType(false, false, false);
    }

    private LocSummary summarize(List<LocFileMetric> files) {
        int totalLines = files.stream().mapToInt(LocFileMetric::totalLines).sum();
        int sourceLines = files.stream().mapToInt(LocFileMetric::sourceLines).sum();
        int commentLines = files.stream().mapToInt(LocFileMetric::commentLines).sum();
        int blankLines = files.stream().mapToInt(LocFileMetric::blankLines).sum();
        return new LocSummary(files.size(), totalLines, sourceLines, commentLines, blankLines, ratio(commentLines, totalLines));
    }

    private double ratio(int value, int total) {
        if (total == 0) {
            return 0;
        }
        return Math.round((value * 10000.0 / total)) / 10000.0;
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-loc-" + timestamp + "-" + suffix;
    }

    private String buildMarkdownReport(LocAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 代码行度量报告\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(result.projectId()).append("\n");
        builder.append("- 任务 ID：").append(result.taskId()).append("\n");
        builder.append("- 分析时间：").append(result.analyzedAt()).append("\n\n");
        builder.append("## 汇总结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| Java 文件数 | ").append(result.summary().fileCount()).append(" |\n");
        builder.append("| 总行数 LoC | ").append(result.summary().totalLines()).append(" |\n");
        builder.append("| 有效代码行 SLoC | ").append(result.summary().sourceLines()).append(" |\n");
        builder.append("| 注释行 | ").append(result.summary().commentLines()).append(" |\n");
        builder.append("| 空行 | ").append(result.summary().blankLines()).append(" |\n");
        builder.append("| 注释率 | ").append(String.format("%.1f%%", result.summary().commentRate() * 100)).append(" |\n\n");
        builder.append("## 文件明细\n\n");
        builder.append("| 文件 | 来源 | 总行 | 有效行 | 注释行 | 空行 | 注释率 |\n");
        builder.append("| --- | --- | ---: | ---: | ---: | ---: | ---: |\n");
        for (LocFileMetric file : result.files()) {
            builder.append("| ")
                    .append(file.fileName()).append(" | ")
                    .append(file.sourceUploadName()).append(" | ")
                    .append(file.totalLines()).append(" | ")
                    .append(file.sourceLines()).append(" | ")
                    .append(file.commentLines()).append(" | ")
                    .append(file.blankLines()).append(" | ")
                    .append(String.format("%.1f%%", file.commentRate() * 100)).append(" |\n");
        }
        return builder.toString();
    }

    private record LineType(boolean blank, boolean commentOnly, boolean inBlockComment) {
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private record ReportMeta(String projectId, String taskId, String reportPath, OffsetDateTime createdAt) {
    }
}
