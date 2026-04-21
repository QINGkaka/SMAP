package com.metriclab.service;

import com.metriclab.model.dto.FunctionPointCount;
import com.metriclab.model.dto.FunctionPointReportResult;
import com.metriclab.model.dto.FunctionPointRequest;
import com.metriclab.model.dto.FunctionPointResult;
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
public class FunctionPointService {

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public FunctionPointService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized FunctionPointResult analyzeProject(String projectId, FunctionPointRequest request) throws IOException {
        uploadService.listFiles(projectId);
        FunctionPointRequest safeRequest = request == null ? emptyRequest() : request;
        int ei = weighted(safeRequest.externalInputs(), 3, 4, 6);
        int eo = weighted(safeRequest.externalOutputs(), 4, 5, 7);
        int eq = weighted(safeRequest.externalInquiries(), 3, 4, 6);
        int ilf = weighted(safeRequest.internalLogicalFiles(), 7, 10, 15);
        int eif = weighted(safeRequest.externalInterfaceFiles(), 5, 7, 10);
        int ufp = ei + eo + eq + ilf + eif;
        int gscTotal = resolveTotal(safeRequest.generalSystemCharacteristics(), safeRequest.generalSystemCharacteristicTotal(), 35, 70);
        double vaf = round2(0.65 + 0.01 * gscTotal);
        double adjusted = round2(ufp * vaf);

        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        FunctionPointResult result = new FunctionPointResult(
                taskId,
                projectId,
                ei,
                eo,
                eq,
                ilf,
                eif,
                ufp,
                gscTotal,
                vaf,
                adjusted,
                now
        );
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "FUNCTION_POINT", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("function-point-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestFunctionPointResultPath(projectId), result);
        return result;
    }

    public FunctionPointResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestFunctionPointResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        return fileStorageService.readJson(latestPath, FunctionPointResult.class);
    }

    public FunctionPointReportResult exportMarkdownReport(String projectId) throws IOException {
        FunctionPointResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有功能点度量结果，请先执行功能点度量");
        }
        String content = buildMarkdown(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("function-point-report.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new FunctionPointReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private FunctionPointRequest emptyRequest() {
        FunctionPointCount empty = new FunctionPointCount(0, 0, 0);
        return new FunctionPointRequest(empty, empty, empty, empty, empty, 35, List.of());
    }

    private int weighted(FunctionPointCount count, int lowWeight, int averageWeight, int highWeight) {
        if (count == null) {
            return 0;
        }
        return Math.max(0, count.low()) * lowWeight
                + Math.max(0, count.average()) * averageWeight
                + Math.max(0, count.high()) * highWeight;
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

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-fp-" + timestamp + "-" + suffix;
    }

    private String buildMarkdown(FunctionPointResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 功能点度量报告\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(result.projectId()).append("\n");
        builder.append("- 任务 ID：").append(result.taskId()).append("\n");
        builder.append("- 分析时间：").append(result.analyzedAt()).append("\n\n");
        builder.append("## 功能点结果\n\n");
        builder.append("| 类别 | 加权功能点 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 外部输入 EI | ").append(result.externalInputs()).append(" |\n");
        builder.append("| 外部输出 EO | ").append(result.externalOutputs()).append(" |\n");
        builder.append("| 外部查询 EQ | ").append(result.externalInquiries()).append(" |\n");
        builder.append("| 内部逻辑文件 ILF | ").append(result.internalLogicalFiles()).append(" |\n");
        builder.append("| 外部接口文件 EIF | ").append(result.externalInterfaceFiles()).append(" |\n");
        builder.append("| 未调整功能点 UFP | ").append(result.unadjustedFunctionPoints()).append(" |\n");
        builder.append("| 通用系统特征总分 | ").append(result.generalSystemCharacteristicTotal()).append(" |\n");
        builder.append("| 调整因子 VAF | ").append(result.valueAdjustmentFactor()).append(" |\n");
        builder.append("| 调整后功能点 AFP | ").append(result.adjustedFunctionPoints()).append(" |\n");
        return builder.toString();
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }
}
