package com.metriclab.service;

import com.metriclab.model.dto.EstimationReportResult;
import com.metriclab.model.dto.EstimationRequest;
import com.metriclab.model.dto.EstimationResult;
import com.metriclab.model.dto.LocAnalysisResult;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class EstimationService {

    private static final double DEFAULT_COST_PER_PERSON_MONTH = 20000.0;

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;
    private final LocAnalysisService locAnalysisService;

    public EstimationService(FileStorageService fileStorageService, UploadService uploadService, LocAnalysisService locAnalysisService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
        this.locAnalysisService = locAnalysisService;
    }

    public synchronized EstimationResult analyzeProject(String projectId, EstimationRequest request) throws IOException {
        uploadService.listFiles(projectId);
        CocomoMode mode = CocomoMode.from(request == null ? null : request.mode());
        double costPerPersonMonth = normalizePositive(
                request == null ? null : request.costPerPersonMonth(),
                DEFAULT_COST_PER_PERSON_MONTH
        );
        Scale scale = resolveScale(projectId, request == null ? null : request.kloc());

        double effort = round2(mode.a() * Math.pow(scale.kloc(), mode.b()));
        double duration = round2(mode.c() * Math.pow(effort, mode.d()));
        double staff = duration <= 0 ? 0 : round2(effort / duration);
        double cost = round2(effort * costPerPersonMonth);

        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        EstimationResult result = new EstimationResult(
                taskId,
                projectId,
                mode.name(),
                mode.label(),
                round2(scale.kloc()),
                scale.source(),
                effort,
                duration,
                staff,
                costPerPersonMonth,
                cost,
                now
        );
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "ESTIMATION", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("estimation-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestEstimationResultPath(projectId), result);
        return result;
    }

    public EstimationResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestEstimationResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        return fileStorageService.readJson(latestPath, EstimationResult.class);
    }

    public EstimationReportResult exportMarkdownReport(String projectId) throws IOException {
        EstimationResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有估算结果，请先执行估算分析");
        }
        String content = buildMarkdownReport(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("estimation-report.md");
        fileStorageService.writeJson(fileStorageService.reportsDirectory(projectId).resolve("estimation-report-meta.json"),
                new ReportMeta(projectId, result.taskId(), reportPath.toString(), OffsetDateTime.now()));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new EstimationReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private Scale resolveScale(String projectId, Double manualKloc) throws IOException {
        if (manualKloc != null && manualKloc > 0) {
            return new Scale(manualKloc, "用户手动输入 KLOC");
        }
        LocAnalysisResult latestLoc = locAnalysisService.latestResult(projectId);
        if (latestLoc == null) {
            latestLoc = locAnalysisService.analyzeProject(projectId);
        }
        double kloc = latestLoc.summary().sourceLines() / 1000.0;
        if (kloc <= 0) {
            throw new IllegalArgumentException("当前项目有效代码行为 0，无法进行工作量估算");
        }
        return new Scale(kloc, "最新 LoC 结果自动换算");
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
        return "task-estimation-" + timestamp + "-" + suffix;
    }

    private String buildMarkdownReport(EstimationResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 工作量与成本估算报告\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(result.projectId()).append("\n");
        builder.append("- 任务 ID：").append(result.taskId()).append("\n");
        builder.append("- 估算时间：").append(result.analyzedAt()).append("\n");
        builder.append("- 模型：基础 COCOMO\n\n");
        builder.append("## 估算参数\n\n");
        builder.append("| 参数 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 项目模式 | ").append(result.modeLabel()).append(" |\n");
        builder.append("| 规模 KLOC | ").append(result.kloc()).append(" |\n");
        builder.append("| 规模来源 | ").append(result.scaleSource()).append(" |\n");
        builder.append("| 人月成本 | ").append(String.format(Locale.ROOT, "%.2f", result.costPerPersonMonth())).append(" |\n\n");
        builder.append("## 估算结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 工作量 | ").append(result.effortPersonMonths()).append(" 人月 |\n");
        builder.append("| 开发周期 | ").append(result.developmentMonths()).append(" 月 |\n");
        builder.append("| 平均人员 | ").append(result.averageStaff()).append(" 人 |\n");
        builder.append("| 估算成本 | ").append(String.format(Locale.ROOT, "%.2f", result.estimatedCost())).append(" |\n");
        return builder.toString();
    }

    private enum CocomoMode {
        ORGANIC("有机型", 2.4, 1.05, 2.5, 0.38),
        SEMIDETACHED("半独立型", 3.0, 1.12, 2.5, 0.35),
        EMBEDDED("嵌入型", 3.6, 1.20, 2.5, 0.32);

        private final String label;
        private final double a;
        private final double b;
        private final double c;
        private final double d;

        CocomoMode(String label, double a, double b, double c, double d) {
            this.label = label;
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        static CocomoMode from(String value) {
            if (value == null || value.isBlank()) {
                return ORGANIC;
            }
            return CocomoMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }

        String label() {
            return label;
        }

        double a() {
            return a;
        }

        double b() {
            return b;
        }

        double c() {
            return c;
        }

        double d() {
            return d;
        }
    }

    private record Scale(double kloc, String source) {
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private record ReportMeta(String projectId, String taskId, String reportPath, OffsetDateTime createdAt) {
    }
}
