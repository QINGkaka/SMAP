package com.metriclab.service;

import com.metriclab.model.dto.FunctionPointComponentSummary;
import com.metriclab.model.dto.FunctionPointCount;
import com.metriclab.model.dto.FunctionPointDetailRequest;
import com.metriclab.model.dto.FunctionPointDetailResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FunctionPointService {

    private static final String MODE_DETAILED = "DETAILED";
    private static final String MODE_ESTIMATED = "ESTIMATED";

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public FunctionPointService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized FunctionPointResult analyzeProject(String projectId, FunctionPointRequest request) throws IOException {
        uploadService.listFiles(projectId);
        FunctionPointRequest safeRequest = request == null ? emptyRequest() : request;
        String countMode = resolveCountMode(safeRequest);

        List<FunctionPointComponentSummary> componentSummaries;
        List<FunctionPointDetailResult> detailItems;
        int ei;
        int eo;
        int eq;
        int ilf;
        int eif;

        if (MODE_DETAILED.equals(countMode)) {
            AnalysisBundle bundle = analyzeDetailedMode(safeRequest);
            componentSummaries = bundle.componentSummaries();
            detailItems = bundle.detailItems();
            ei = bundle.points("EI");
            eo = bundle.points("EO");
            eq = bundle.points("EQ");
            ilf = bundle.points("ILF");
            eif = bundle.points("EIF");
        } else {
            componentSummaries = buildEstimatedSummaries(safeRequest);
            detailItems = List.of();
            ei = weighted(safeRequest.externalInputs(), 3, 4, 6);
            eo = weighted(safeRequest.externalOutputs(), 4, 5, 7);
            eq = weighted(safeRequest.externalInquiries(), 3, 4, 6);
            ilf = weighted(safeRequest.internalLogicalFiles(), 7, 10, 15);
            eif = weighted(safeRequest.externalInterfaceFiles(), 5, 7, 10);
        }

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
                countMode,
                componentSummaries,
                detailItems,
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
        return new FunctionPointRequest(
                empty,
                empty,
                empty,
                empty,
                empty,
                MODE_DETAILED,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                35,
                List.of()
        );
    }

    private String resolveCountMode(FunctionPointRequest request) {
        String mode = request.countMode();
        if (mode != null && MODE_ESTIMATED.equalsIgnoreCase(mode.trim())) {
            return MODE_ESTIMATED;
        }
        if (hasDetailItems(request)) {
            return MODE_DETAILED;
        }
        return MODE_DETAILED.equalsIgnoreCase(mode == null ? "" : mode.trim()) ? MODE_DETAILED : MODE_ESTIMATED;
    }

    private boolean hasDetailItems(FunctionPointRequest request) {
        return hasNonEmptyDetails(request.externalInputDetails())
                || hasNonEmptyDetails(request.externalOutputDetails())
                || hasNonEmptyDetails(request.externalInquiryDetails())
                || hasNonEmptyDetails(request.internalLogicalFileDetails())
                || hasNonEmptyDetails(request.externalInterfaceFileDetails());
    }

    private boolean hasNonEmptyDetails(List<FunctionPointDetailRequest> details) {
        return details != null && details.stream().anyMatch(item -> !isEmptyDetail(item));
    }

    private AnalysisBundle analyzeDetailedMode(FunctionPointRequest request) {
        List<FunctionPointComponentSummary> componentSummaries = new ArrayList<>();
        List<FunctionPointDetailResult> detailItems = new ArrayList<>();

        ComponentBundle ei = analyzeTransactionDetails("EI", "外部输入", request.externalInputDetails(), 3, 4, 6, true);
        ComponentBundle eo = analyzeTransactionDetails("EO", "外部输出", request.externalOutputDetails(), 4, 5, 7, false);
        ComponentBundle eq = analyzeTransactionDetails("EQ", "外部查询", request.externalInquiryDetails(), 3, 4, 6, false);
        ComponentBundle ilf = analyzeDataFunctionDetails("ILF", "内部逻辑文件", request.internalLogicalFileDetails(), 7, 10, 15);
        ComponentBundle eif = analyzeDataFunctionDetails("EIF", "外部接口文件", request.externalInterfaceFileDetails(), 5, 7, 10);

        componentSummaries.add(ei.summary());
        componentSummaries.add(eo.summary());
        componentSummaries.add(eq.summary());
        componentSummaries.add(ilf.summary());
        componentSummaries.add(eif.summary());

        detailItems.addAll(ei.details());
        detailItems.addAll(eo.details());
        detailItems.addAll(eq.details());
        detailItems.addAll(ilf.details());
        detailItems.addAll(eif.details());

        return new AnalysisBundle(List.copyOf(componentSummaries), List.copyOf(detailItems));
    }

    private ComponentBundle analyzeTransactionDetails(
            String code,
            String label,
            List<FunctionPointDetailRequest> details,
            int lowWeight,
            int averageWeight,
            int highWeight,
            boolean externalInput
    ) {
        List<FunctionPointDetailResult> results = new ArrayList<>();
        int lowCount = 0;
        int averageCount = 0;
        int highCount = 0;
        int totalPoints = 0;
        int index = 1;
        for (FunctionPointDetailRequest detail : normalizeDetails(details)) {
            int det = requirePositiveMetric(detail.det(), code, index, "DET");
            int ftr = requireNonNegativeMetric(detail.ftr(), code, index, "FTR");
            ComplexityLevel level = externalInput ? classifyExternalInput(det, ftr) : classifyOutputOrInquiry(det, ftr);
            int points = level.weight(lowWeight, averageWeight, highWeight);
            if (level == ComplexityLevel.LOW) {
                lowCount++;
            } else if (level == ComplexityLevel.AVERAGE) {
                averageCount++;
            } else {
                highCount++;
            }
            totalPoints += points;
            results.add(new FunctionPointDetailResult(
                    code,
                    label,
                    resolveDetailName(detail.name(), code, index),
                    det,
                    null,
                    ftr,
                    level.displayName(),
                    points
            ));
            index++;
        }
        return new ComponentBundle(
                new FunctionPointComponentSummary(code, label, results.size(), lowCount, averageCount, highCount, totalPoints),
                List.copyOf(results)
        );
    }

    private ComponentBundle analyzeDataFunctionDetails(
            String code,
            String label,
            List<FunctionPointDetailRequest> details,
            int lowWeight,
            int averageWeight,
            int highWeight
    ) {
        List<FunctionPointDetailResult> results = new ArrayList<>();
        int lowCount = 0;
        int averageCount = 0;
        int highCount = 0;
        int totalPoints = 0;
        int index = 1;
        for (FunctionPointDetailRequest detail : normalizeDetails(details)) {
            int det = requirePositiveMetric(detail.det(), code, index, "DET");
            int ret = requirePositiveMetric(detail.ret(), code, index, "RET");
            ComplexityLevel level = classifyDataFunction(det, ret);
            int points = level.weight(lowWeight, averageWeight, highWeight);
            if (level == ComplexityLevel.LOW) {
                lowCount++;
            } else if (level == ComplexityLevel.AVERAGE) {
                averageCount++;
            } else {
                highCount++;
            }
            totalPoints += points;
            results.add(new FunctionPointDetailResult(
                    code,
                    label,
                    resolveDetailName(detail.name(), code, index),
                    det,
                    ret,
                    null,
                    level.displayName(),
                    points
            ));
            index++;
        }
        return new ComponentBundle(
                new FunctionPointComponentSummary(code, label, results.size(), lowCount, averageCount, highCount, totalPoints),
                List.copyOf(results)
        );
    }

    private List<FunctionPointComponentSummary> buildEstimatedSummaries(FunctionPointRequest request) {
        return List.of(
                buildEstimatedSummary("EI", "外部输入", request.externalInputs(), 3, 4, 6),
                buildEstimatedSummary("EO", "外部输出", request.externalOutputs(), 4, 5, 7),
                buildEstimatedSummary("EQ", "外部查询", request.externalInquiries(), 3, 4, 6),
                buildEstimatedSummary("ILF", "内部逻辑文件", request.internalLogicalFiles(), 7, 10, 15),
                buildEstimatedSummary("EIF", "外部接口文件", request.externalInterfaceFiles(), 5, 7, 10)
        );
    }

    private FunctionPointComponentSummary buildEstimatedSummary(
            String code,
            String label,
            FunctionPointCount count,
            int lowWeight,
            int averageWeight,
            int highWeight
    ) {
        int low = count == null ? 0 : Math.max(0, count.low());
        int average = count == null ? 0 : Math.max(0, count.average());
        int high = count == null ? 0 : Math.max(0, count.high());
        int points = low * lowWeight + average * averageWeight + high * highWeight;
        return new FunctionPointComponentSummary(code, label, low + average + high, low, average, high, points);
    }

    private List<FunctionPointDetailRequest> normalizeDetails(List<FunctionPointDetailRequest> details) {
        if (details == null || details.isEmpty()) {
            return List.of();
        }
        return details.stream()
                .filter(item -> !isEmptyDetail(item))
                .toList();
    }

    private boolean isEmptyDetail(FunctionPointDetailRequest item) {
        if (item == null) {
            return true;
        }
        boolean noName = item.name() == null || item.name().isBlank();
        boolean noDet = item.det() == null || item.det() <= 0;
        boolean noRet = item.ret() == null || item.ret() <= 0;
        boolean noFtr = item.ftr() == null || item.ftr() <= 0;
        return noName && noDet && noRet && noFtr;
    }

    private int requirePositiveMetric(Integer value, String code, int index, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(code + " 第 " + index + " 项缺少有效的 " + field + " 值");
        }
        return value;
    }

    private int requireNonNegativeMetric(Integer value, String code, int index, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(code + " 第 " + index + " 项缺少有效的 " + field + " 值");
        }
        return value;
    }

    private String resolveDetailName(String name, String code, int index) {
        if (name == null || name.isBlank()) {
            return code + "-" + index;
        }
        return name.trim();
    }

    private ComplexityLevel classifyExternalInput(int det, int ftr) {
        if (det <= 4) {
            return ftr >= 3 ? ComplexityLevel.AVERAGE : ComplexityLevel.LOW;
        }
        if (det <= 15) {
            if (ftr <= 1) {
                return ComplexityLevel.LOW;
            }
            return ftr == 2 ? ComplexityLevel.AVERAGE : ComplexityLevel.HIGH;
        }
        if (ftr <= 1) {
            return ComplexityLevel.AVERAGE;
        }
        return ComplexityLevel.HIGH;
    }

    private ComplexityLevel classifyOutputOrInquiry(int det, int ftr) {
        if (det <= 5) {
            if (ftr <= 3) {
                return ComplexityLevel.LOW;
            }
            return ComplexityLevel.AVERAGE;
        }
        if (det <= 19) {
            if (ftr <= 1) {
                return ComplexityLevel.LOW;
            }
            return ftr <= 3 ? ComplexityLevel.AVERAGE : ComplexityLevel.HIGH;
        }
        if (ftr <= 1) {
            return ComplexityLevel.AVERAGE;
        }
        return ComplexityLevel.HIGH;
    }

    private ComplexityLevel classifyDataFunction(int det, int ret) {
        if (det <= 19) {
            if (ret <= 5) {
                return ComplexityLevel.LOW;
            }
            return ComplexityLevel.AVERAGE;
        }
        if (det <= 50) {
            if (ret == 1) {
                return ComplexityLevel.LOW;
            }
            return ret <= 5 ? ComplexityLevel.AVERAGE : ComplexityLevel.HIGH;
        }
        if (ret == 1) {
            return ComplexityLevel.AVERAGE;
        }
        return ComplexityLevel.HIGH;
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
        builder.append("- 分析时间：").append(result.analyzedAt()).append("\n");
        builder.append("- 计数方式：").append(MODE_DETAILED.equalsIgnoreCase(result.countMode()) ? "详细计数（DET/FTR/RET 自动判级）" : "预估算（低/中/高人工录入）").append("\n\n");

        builder.append("## 功能点结果\n\n");
        builder.append("| 类别 | 项数 | 低 | 中 | 高 | 加权功能点 |\n");
        builder.append("| --- | ---: | ---: | ---: | ---: | ---: |\n");
        if (result.componentSummaries() != null && !result.componentSummaries().isEmpty()) {
            for (FunctionPointComponentSummary item : result.componentSummaries()) {
                builder.append("| ").append(item.label()).append(" ").append(item.code())
                        .append(" | ").append(item.itemCount())
                        .append(" | ").append(item.lowCount())
                        .append(" | ").append(item.averageCount())
                        .append(" | ").append(item.highCount())
                        .append(" | ").append(item.functionPoints())
                        .append(" |\n");
            }
        } else {
            builder.append("| 外部输入 EI | - | - | - | - | ").append(result.externalInputs()).append(" |\n");
            builder.append("| 外部输出 EO | - | - | - | - | ").append(result.externalOutputs()).append(" |\n");
            builder.append("| 外部查询 EQ | - | - | - | - | ").append(result.externalInquiries()).append(" |\n");
            builder.append("| 内部逻辑文件 ILF | - | - | - | - | ").append(result.internalLogicalFiles()).append(" |\n");
            builder.append("| 外部接口文件 EIF | - | - | - | - | ").append(result.externalInterfaceFiles()).append(" |\n");
        }
        builder.append("| 未调整功能点 UFP |  |  |  |  | ").append(result.unadjustedFunctionPoints()).append(" |\n");
        builder.append("| 通用系统特征总分 GSC |  |  |  |  | ").append(result.generalSystemCharacteristicTotal()).append(" |\n");
        builder.append("| 调整因子 VAF |  |  |  |  | ").append(result.valueAdjustmentFactor()).append(" |\n");
        builder.append("| 调整后功能点 AFP |  |  |  |  | ").append(result.adjustedFunctionPoints()).append(" |\n");

        if (result.detailItems() != null && !result.detailItems().isEmpty()) {
        builder.append("\n## 详细计数明细\n\n");
        builder.append("| 类别 | 功能项 | DET/DER | FTR/RET | 复杂度 | 功能点 |\n");
            builder.append("| --- | --- | ---: | ---: | --- | ---: |\n");
            for (FunctionPointDetailResult item : result.detailItems()) {
                Integer relationValue = item.ftr() != null ? item.ftr() : item.ret();
                builder.append("| ").append(item.code())
                        .append(" | ").append(item.name())
                        .append(" | ").append(item.det())
                        .append(" | ").append(relationValue == null ? "-" : relationValue)
                        .append(" | ").append(item.complexity())
                        .append(" | ").append(item.functionPoints())
                        .append(" |\n");
            }
        }

        builder.append("\n## 判级规则说明\n\n");
        builder.append("1. 事务功能（EI、EO、EQ）按照课件中的 `DER + FTR` 组合规则自动判定低、中、高复杂度。\n");
        builder.append("2. 数据功能（ILF、EIF）按照课件中的 `DET + RET` 组合规则自动判定低、中、高复杂度。\n");
        builder.append("3. 最终按 IFPUG 标准权重累加未调整功能点，再使用 `VAF = 0.65 + 0.01 * GSC` 计算调整后功能点。\n");
        return builder.toString();
    }

    private enum ComplexityLevel {
        LOW("低"),
        AVERAGE("中"),
        HIGH("高");

        private final String displayName;

        ComplexityLevel(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }

        int weight(int lowWeight, int averageWeight, int highWeight) {
            return switch (this) {
                case LOW -> lowWeight;
                case AVERAGE -> averageWeight;
                case HIGH -> highWeight;
            };
        }
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private record ComponentBundle(FunctionPointComponentSummary summary, List<FunctionPointDetailResult> details) {
    }

    private record AnalysisBundle(List<FunctionPointComponentSummary> componentSummaries, List<FunctionPointDetailResult> detailItems) {
        int points(String code) {
            return componentSummaries.stream()
                    .filter(item -> code.equalsIgnoreCase(item.code()))
                    .mapToInt(FunctionPointComponentSummary::functionPoints)
                    .findFirst()
                    .orElse(0);
        }
    }
}
