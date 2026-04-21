package com.metriclab.service;

import com.metriclab.model.dto.AiAnalysisResult;
import com.metriclab.model.dto.ClassMetric;
import com.metriclab.model.dto.ComplexityAnalysisResult;
import com.metriclab.model.dto.ComprehensiveReportResult;
import com.metriclab.model.dto.EstimationResult;
import com.metriclab.model.dto.FunctionPointResult;
import com.metriclab.model.dto.LocAnalysisResult;
import com.metriclab.model.dto.MethodComplexityMetric;
import com.metriclab.model.dto.ModelAnalysisResult;
import com.metriclab.model.dto.ModelClassMetric;
import com.metriclab.model.dto.ObjectOrientedAnalysisResult;
import com.metriclab.model.dto.UseCasePointResult;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ComprehensiveReportService {

    private final FileStorageService fileStorageService;
    private final LocAnalysisService locAnalysisService;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final ObjectOrientedAnalysisService objectOrientedAnalysisService;
    private final EstimationService estimationService;
    private final AiAnalysisService aiAnalysisService;
    private final FunctionPointService functionPointService;
    private final UseCasePointService useCasePointService;
    private final ModelAnalysisService modelAnalysisService;

    public ComprehensiveReportService(
            FileStorageService fileStorageService,
            LocAnalysisService locAnalysisService,
            ComplexityAnalysisService complexityAnalysisService,
            ObjectOrientedAnalysisService objectOrientedAnalysisService,
            EstimationService estimationService,
            AiAnalysisService aiAnalysisService,
            FunctionPointService functionPointService,
            UseCasePointService useCasePointService,
            ModelAnalysisService modelAnalysisService
    ) {
        this.fileStorageService = fileStorageService;
        this.locAnalysisService = locAnalysisService;
        this.complexityAnalysisService = complexityAnalysisService;
        this.objectOrientedAnalysisService = objectOrientedAnalysisService;
        this.estimationService = estimationService;
        this.aiAnalysisService = aiAnalysisService;
        this.functionPointService = functionPointService;
        this.useCasePointService = useCasePointService;
        this.modelAnalysisService = modelAnalysisService;
    }

    public synchronized ComprehensiveReportResult export(String projectId) throws IOException {
        LocAnalysisResult loc = latestOrAnalyzeLoc(projectId);
        ComplexityAnalysisResult complexity = latestOrAnalyzeComplexity(projectId);
        ObjectOrientedAnalysisResult oo = latestOrAnalyzeObjectOriented(projectId);
        EstimationResult estimation = latestOrAnalyzeEstimation(projectId);
        AiAnalysisResult ai = latestOrAnalyzeAi(projectId);
        FunctionPointResult functionPoint = functionPointService.latestResult(projectId);
        UseCasePointResult useCasePoint = useCasePointService.latestResult(projectId);
        ModelAnalysisResult modelAnalysis = modelAnalysisService.latestResult(projectId);

        String content = buildReport(projectId, loc, complexity, oo, estimation, ai, functionPoint, useCasePoint, modelAnalysis);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("final-metric-report.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new ComprehensiveReportResult(projectId, reportPath.toString(), content, OffsetDateTime.now());
    }

    private LocAnalysisResult latestOrAnalyzeLoc(String projectId) throws IOException {
        LocAnalysisResult result = locAnalysisService.latestResult(projectId);
        return result == null ? locAnalysisService.analyzeProject(projectId) : result;
    }

    private ComplexityAnalysisResult latestOrAnalyzeComplexity(String projectId) throws IOException {
        ComplexityAnalysisResult result = complexityAnalysisService.latestResult(projectId);
        return result == null ? complexityAnalysisService.analyzeProject(projectId) : result;
    }

    private ObjectOrientedAnalysisResult latestOrAnalyzeObjectOriented(String projectId) throws IOException {
        ObjectOrientedAnalysisResult result = objectOrientedAnalysisService.latestResult(projectId);
        return result == null ? objectOrientedAnalysisService.analyzeProject(projectId) : result;
    }

    private EstimationResult latestOrAnalyzeEstimation(String projectId) throws IOException {
        EstimationResult result = estimationService.latestResult(projectId);
        return result == null ? estimationService.analyzeProject(projectId, null) : result;
    }

    private AiAnalysisResult latestOrAnalyzeAi(String projectId) throws IOException {
        AiAnalysisResult result = aiAnalysisService.latestResult(projectId);
        return result == null ? aiAnalysisService.analyzeProject(projectId) : result;
    }

    private String buildReport(
            String projectId,
            LocAnalysisResult loc,
            ComplexityAnalysisResult complexity,
            ObjectOrientedAnalysisResult oo,
            EstimationResult estimation,
            AiAnalysisResult ai,
            FunctionPointResult functionPoint,
            UseCasePointResult useCasePoint,
            ModelAnalysisResult modelAnalysis
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 软件度量自动化工具实验结果报告\n\n");
        builder.append("## 1. 项目概述\n\n");
        builder.append("本报告由 JavaMetricLab 自动生成，汇总项目 `").append(projectId)
                .append("` 的代码行、圈复杂度、CK/LK 面向对象度量、基础 COCOMO 估算和智能质量分析结果。\n\n");
        builder.append("## 2. 规模度量结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| Java 文件数 | ").append(loc.summary().fileCount()).append(" |\n");
        builder.append("| 总行数 LoC | ").append(loc.summary().totalLines()).append(" |\n");
        builder.append("| 有效代码行 SLoC | ").append(loc.summary().sourceLines()).append(" |\n");
        builder.append("| 注释行 | ").append(loc.summary().commentLines()).append(" |\n");
        builder.append("| 空行 | ").append(loc.summary().blankLines()).append(" |\n");
        builder.append("| 注释率 | ").append(String.format("%.1f%%", loc.summary().commentRate() * 100)).append(" |\n\n");

        builder.append("## 3. 圈复杂度结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 方法数 | ").append(complexity.summary().methodCount()).append(" |\n");
        builder.append("| 平均圈复杂度 | ").append(complexity.summary().averageComplexity()).append(" |\n");
        builder.append("| 最高圈复杂度 | ").append(complexity.summary().maxComplexity()).append(" |\n");
        builder.append("| 高风险方法数 | ").append(complexity.summary().highRiskMethodCount()).append(" |\n\n");
        appendTopMethods(builder, complexity.methods());

        builder.append("## 4. CK/LK 面向对象度量结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 类数量 | ").append(oo.summary().classCount()).append(" |\n");
        builder.append("| 接口数量 | ").append(oo.summary().interfaceCount()).append(" |\n");
        builder.append("| 方法数量 | ").append(oo.summary().methodCount()).append(" |\n");
        builder.append("| 字段数量 | ").append(oo.summary().fieldCount()).append(" |\n");
        builder.append("| 平均 CBO | ").append(oo.summary().averageCbo()).append(" |\n");
        builder.append("| 最大 DIT | ").append(oo.summary().maxDit()).append(" |\n");
        builder.append("| 高风险类数 | ").append(oo.summary().highRiskClassCount()).append(" |\n\n");
        appendTopClasses(builder, oo.classes());

        builder.append("## 5. 工作量与成本估算\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 项目模式 | ").append(estimation.modeLabel()).append(" |\n");
        builder.append("| 规模 KLOC | ").append(estimation.kloc()).append(" |\n");
        builder.append("| 工作量 | ").append(estimation.effortPersonMonths()).append(" 人月 |\n");
        builder.append("| 开发周期 | ").append(estimation.developmentMonths()).append(" 月 |\n");
        builder.append("| 平均人员 | ").append(estimation.averageStaff()).append(" 人 |\n");
        builder.append("| 估算成本 | ").append(estimation.estimatedCost()).append(" |\n\n");

        if (functionPoint != null) {
            builder.append("## 6. 功能点度量结果\n\n");
            builder.append("| 指标 | 数值 |\n");
            builder.append("| --- | ---: |\n");
            builder.append("| 未调整功能点 UFP | ").append(functionPoint.unadjustedFunctionPoints()).append(" |\n");
            builder.append("| 通用系统特征总分 | ").append(functionPoint.generalSystemCharacteristicTotal()).append(" |\n");
            builder.append("| 调整因子 VAF | ").append(functionPoint.valueAdjustmentFactor()).append(" |\n");
            builder.append("| 调整后功能点 AFP | ").append(functionPoint.adjustedFunctionPoints()).append(" |\n\n");
        }

        if (useCasePoint != null) {
            builder.append("## 7. 用例点估算结果\n\n");
            builder.append("| 指标 | 数值 |\n");
            builder.append("| --- | ---: |\n");
            builder.append("| 参与者权重 UAW | ").append(useCasePoint.actorWeight()).append(" |\n");
            builder.append("| 用例权重 UUCW | ").append(useCasePoint.useCaseWeight()).append(" |\n");
            builder.append("| 未调整用例点 UUCP | ").append(useCasePoint.unadjustedUseCasePoints()).append(" |\n");
            builder.append("| 用例点 UCP | ").append(useCasePoint.useCasePoints()).append(" |\n");
            builder.append("| 估算工时 | ").append(useCasePoint.estimatedHours()).append(" 小时 |\n");
            builder.append("| 估算人月 | ").append(useCasePoint.estimatedPersonMonths()).append(" 人月 |\n\n");
        }

        if (modelAnalysis != null) {
            builder.append("## 8. 模型文件度量结果\n\n");
            builder.append("| 指标 | 数值 |\n");
            builder.append("| --- | ---: |\n");
            builder.append("| 模型文件数 | ").append(modelAnalysis.summary().fileCount()).append(" |\n");
            builder.append("| 类数量 | ").append(modelAnalysis.summary().classCount()).append(" |\n");
            builder.append("| 接口数量 | ").append(modelAnalysis.summary().interfaceCount()).append(" |\n");
            builder.append("| 属性数量 | ").append(modelAnalysis.summary().attributeCount()).append(" |\n");
            builder.append("| 操作数量 | ").append(modelAnalysis.summary().operationCount()).append(" |\n");
            builder.append("| 继承关系数量 | ").append(modelAnalysis.summary().inheritanceRelationCount()).append(" |\n");
            builder.append("| 高风险类数 | ").append(modelAnalysis.summary().highRiskClassCount()).append(" |\n\n");
            appendTopModelClasses(builder, modelAnalysis.classes());
        }

        builder.append(modelAnalysis == null ? "## 8. 智能分析结论\n\n" : "## 9. 智能分析结论\n\n");
        builder.append(ai.overallAssessment()).append("\n\n");
        appendList(builder, "主要风险", ai.riskItems());
        appendList(builder, "重构建议", ai.refactoringSuggestions());
        appendList(builder, "测试建议", ai.testSuggestions());

        builder.append(modelAnalysis == null ? "## 9. 总结\n\n" : "## 10. 总结\n\n");
        builder.append("本次实验已经覆盖 Java 源码输入、规模统计、控制流复杂度、CK/LK 面向对象指标、模型文件度量、功能点/用例点估算、工作量成本估算、XML 导出和智能分析建议。")
                .append("后续可根据课堂数据集继续校准阈值、权重和生产率参数。\n");
        return builder.toString();
    }

    private void appendTopMethods(StringBuilder builder, List<MethodComplexityMetric> methods) {
        builder.append("复杂度最高方法 Top 5：\n\n");
        builder.append("| 方法 | 文件 | 圈复杂度 | 风险 |\n");
        builder.append("| --- | --- | ---: | --- |\n");
        methods.stream()
                .sorted(Comparator.comparingInt(MethodComplexityMetric::cyclomaticComplexity).reversed())
                .limit(5)
                .forEach(method -> builder.append("| ")
                        .append(method.methodName()).append(" | ")
                        .append(method.fileName()).append(" | ")
                        .append(method.cyclomaticComplexity()).append(" | ")
                        .append(method.riskLevel()).append(" |\n"));
        builder.append("\n");
    }

    private void appendTopClasses(StringBuilder builder, List<ClassMetric> classes) {
        builder.append("综合风险类 Top 5：\n\n");
        builder.append("| 类 | CBO | DIT | WMC | LCOM | 风险 |\n");
        builder.append("| --- | ---: | ---: | ---: | ---: | --- |\n");
        classes.stream()
                .sorted(Comparator.comparingInt(this::classRiskScore).reversed())
                .limit(5)
                .forEach(item -> builder.append("| ")
                        .append(item.className()).append(" | ")
                        .append(item.cbo()).append(" | ")
                        .append(item.dit()).append(" | ")
                        .append(item.wmc()).append(" | ")
                        .append(item.lcom()).append(" | ")
                        .append(item.riskLevel()).append(" |\n"));
        builder.append("\n");
    }

    private void appendTopModelClasses(StringBuilder builder, List<ModelClassMetric> classes) {
        builder.append("模型风险类 Top 5：\n\n");
        builder.append("| 类/接口 | 属性 | 操作 | 子类 | 继承深度 | 风险 |\n");
        builder.append("| --- | ---: | ---: | ---: | ---: | --- |\n");
        classes.stream()
                .sorted(Comparator.comparingInt(this::modelClassRiskScore).reversed())
                .limit(5)
                .forEach(item -> builder.append("| ")
                        .append(item.className()).append(" | ")
                        .append(item.attributeCount()).append(" | ")
                        .append(item.operationCount()).append(" | ")
                        .append(item.childCount()).append(" | ")
                        .append(item.inheritanceDepth()).append(" | ")
                        .append(item.riskLevel()).append(" |\n"));
        builder.append("\n");
    }

    private void appendList(StringBuilder builder, String title, List<String> items) {
        builder.append("### ").append(title).append("\n\n");
        for (String item : items) {
            builder.append("- ").append(item).append("\n");
        }
        builder.append("\n");
    }

    private int classRiskScore(ClassMetric item) {
        return item.cbo() * 3 + item.wmc() + item.lcom() + item.dit() * 2;
    }

    private int modelClassRiskScore(ModelClassMetric item) {
        return item.classSize() + item.childCount() * 3 + item.inheritanceDepth() * 4;
    }
}
