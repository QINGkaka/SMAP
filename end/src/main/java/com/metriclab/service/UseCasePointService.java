package com.metriclab.service;

import com.metriclab.model.dto.UseCasePointReportResult;
import com.metriclab.model.dto.UseCasePointRequest;
import com.metriclab.model.dto.UseCasePointResult;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class UseCasePointService {

    private static final double DEFAULT_PRODUCTIVITY_HOURS = 20.0;
    private static final double HOURS_PER_PERSON_MONTH = 160.0;

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public UseCasePointService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized UseCasePointResult analyzeProject(String projectId, UseCasePointRequest request) throws IOException {
        uploadService.listFiles(projectId);
        UseCasePointRequest safeRequest = request == null ? defaultRequest() : request;
        int actorWeight = nonNegative(safeRequest.simpleActors()) * 1
                + nonNegative(safeRequest.averageActors()) * 2
                + nonNegative(safeRequest.complexActors()) * 3;
        int useCaseWeight = nonNegative(safeRequest.simpleUseCases()) * 5
                + nonNegative(safeRequest.averageUseCases()) * 10
                + nonNegative(safeRequest.complexUseCases()) * 15;
        int uucp = actorWeight + useCaseWeight;
        int technicalTotal = resolveTotal(safeRequest.technicalFactors(), safeRequest.technicalFactorTotal(), 30, 65);
        int environmentalTotal = resolveTotal(safeRequest.environmentalFactors(), safeRequest.environmentalFactorTotal(), 20, 40);
        double tcf = round2(0.6 + 0.01 * technicalTotal);
        double ecf = round2(1.4 - 0.03 * environmentalTotal);
        double ucp = round2(uucp * tcf * ecf);
        double productivity = normalizePositive(safeRequest.productivityHoursPerUseCasePoint(), DEFAULT_PRODUCTIVITY_HOURS);
        double hours = round2(ucp * productivity);
        double personMonths = round2(hours / HOURS_PER_PERSON_MONTH);

        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        UseCasePointResult result = new UseCasePointResult(
                taskId,
                projectId,
                actorWeight,
                useCaseWeight,
                uucp,
                technicalTotal,
                tcf,
                environmentalTotal,
                ecf,
                ucp,
                productivity,
                hours,
                personMonths,
                now
        );
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "USE_CASE_POINT", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("use-case-point-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestUseCasePointResultPath(projectId), result);
        return result;
    }

    public UseCasePointResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestUseCasePointResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        return fileStorageService.readJson(latestPath, UseCasePointResult.class);
    }

    public UseCasePointReportResult exportMarkdownReport(String projectId) throws IOException {
        UseCasePointResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有用例点估算结果，请先执行用例点估算");
        }
        String content = buildMarkdown(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("use-case-point-report.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new UseCasePointReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private UseCasePointRequest defaultRequest() {
        return new UseCasePointRequest(1, 1, 0, 2, 1, 0, 30, 20, DEFAULT_PRODUCTIVITY_HOURS, List.of(), List.of());
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private int resolveTotal(List<Integer> values, Integer fallback, int defaultValue, int max) {
        if (values != null && !values.isEmpty()) {
            return clamp(values.stream().mapToInt(value -> clamp(value == null ? 0 : value, 0, 5)).sum(), 0, max);
        }
        return clamp(fallback == null ? defaultValue : fallback, 0, max);
    }

    private double normalizePositive(Double value, double defaultValue) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return value;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-ucp-" + timestamp + "-" + suffix;
    }

    private String buildMarkdown(UseCasePointResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 用例点估算报告\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(result.projectId()).append("\n");
        builder.append("- 任务 ID：").append(result.taskId()).append("\n");
        builder.append("- 分析时间：").append(result.analyzedAt()).append("\n\n");
        builder.append("## 估算结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 参与者权重 UAW | ").append(result.actorWeight()).append(" |\n");
        builder.append("| 用例权重 UUCW | ").append(result.useCaseWeight()).append(" |\n");
        builder.append("| 未调整用例点 UUCP | ").append(result.unadjustedUseCasePoints()).append(" |\n");
        builder.append("| 技术复杂度因子 TCF | ").append(result.technicalComplexityFactor()).append(" |\n");
        builder.append("| 环境复杂度因子 ECF | ").append(result.environmentalComplexityFactor()).append(" |\n");
        builder.append("| 用例点 UCP | ").append(result.useCasePoints()).append(" |\n");
        builder.append("| 生产率 | ").append(result.productivityHoursPerUseCasePoint()).append(" 小时/用例点 |\n");
        builder.append("| 估算工时 | ").append(result.estimatedHours()).append(" 小时 |\n");
        builder.append("| 估算人月 | ").append(result.estimatedPersonMonths()).append(" 人月 |\n");
        return builder.toString();
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }
}
