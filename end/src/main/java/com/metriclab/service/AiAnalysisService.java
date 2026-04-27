package com.metriclab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metriclab.config.LargeModelProperties;
import com.metriclab.model.dto.AiAnalysisResult;
import com.metriclab.model.dto.ClassMetric;
import com.metriclab.model.dto.ComplexityAnalysisResult;
import com.metriclab.model.dto.EstimationResult;
import com.metriclab.model.dto.FunctionPointResult;
import com.metriclab.model.dto.LocAnalysisResult;
import com.metriclab.model.dto.MethodComplexityMetric;
import com.metriclab.model.dto.ObjectOrientedAnalysisResult;
import com.metriclab.model.dto.UseCasePointResult;
import com.metriclab.storage.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final FileStorageService fileStorageService;
    private final LocAnalysisService locAnalysisService;
    private final ComplexityAnalysisService complexityAnalysisService;
    private final ObjectOrientedAnalysisService objectOrientedAnalysisService;
    private final EstimationService estimationService;
    private final FunctionPointService functionPointService;
    private final UseCasePointService useCasePointService;
    private final LargeModelProperties largeModelProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiAnalysisService(
            FileStorageService fileStorageService,
            LocAnalysisService locAnalysisService,
            ComplexityAnalysisService complexityAnalysisService,
            ObjectOrientedAnalysisService objectOrientedAnalysisService,
            EstimationService estimationService,
            FunctionPointService functionPointService,
            UseCasePointService useCasePointService,
            LargeModelProperties largeModelProperties
    ) {
        this.fileStorageService = fileStorageService;
        this.locAnalysisService = locAnalysisService;
        this.complexityAnalysisService = complexityAnalysisService;
        this.objectOrientedAnalysisService = objectOrientedAnalysisService;
        this.estimationService = estimationService;
        this.functionPointService = functionPointService;
        this.useCasePointService = useCasePointService;
        this.largeModelProperties = largeModelProperties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, largeModelProperties.getTimeoutSeconds())))
                .build();
    }

    @PostConstruct
    public void logLargeModelConfiguration() {
        log.info("Large model configuration loaded: enabled={}, effectiveEnabled={}, provider={}, apiKeyConfigured={}, model={}, baseUrlConfigured={}, reasoningEffort={}, maxCompletionTokens={}",
                largeModelProperties.isEnabled(),
                isLargeModelConfigured(),
                largeModelProperties.getProvider(),
                !isBlank(largeModelProperties.getApiKey()),
                largeModelProperties.getModel(),
                !isBlank(largeModelProperties.getBaseUrl()),
                largeModelProperties.getReasoningEffort(),
                largeModelProperties.getMaxCompletionTokens());
    }

    public synchronized AiAnalysisResult analyzeProject(String projectId) throws IOException {
        LocAnalysisResult loc = ensureLoc(projectId);
        ComplexityAnalysisResult complexity = ensureComplexity(projectId);
        ObjectOrientedAnalysisResult oo = ensureObjectOriented(projectId);
        EstimationResult estimation = ensureEstimation(projectId);
        FunctionPointResult functionPoint = functionPointService.latestResult(projectId);
        UseCasePointResult useCasePoint = useCasePointService.latestResult(projectId);

        List<String> risks = buildRisks(loc, complexity, oo);
        List<String> refactoring = buildRefactoringSuggestions(complexity, oo);
        List<String> tests = buildTestSuggestions(complexity, oo);
        String assessment = buildAssessment(loc, complexity, oo, estimation, risks);
        String modelName = "LocalRuleAnalyzer";

        LargeModelAnswer largeModelAnswer = tryAnalyzeWithLargeModel(projectId, loc, complexity, oo, estimation, functionPoint, useCasePoint);
        if (largeModelAnswer != null) {
            modelName = largeModelAnswer.modelName();
            assessment = largeModelAnswer.overallAssessment();
            risks = largeModelAnswer.riskItems();
            refactoring = largeModelAnswer.refactoringSuggestions();
            tests = largeModelAnswer.testSuggestions();
        }
        String markdown = buildMarkdown(projectId, loc, complexity, oo, estimation, assessment, risks, refactoring, tests, modelName);

        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        AiAnalysisResult result = new AiAnalysisResult(
                taskId,
                projectId,
                modelName,
                assessment,
                risks,
                refactoring,
                tests,
                markdown,
                now
        );
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "AI_ANALYSIS", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("ai-analysis.json"), result);
        Files.createDirectories(taskDirectory);
        Files.writeString(taskDirectory.resolve("ai-analysis.md"), markdown, StandardCharsets.UTF_8);
        fileStorageService.writeJson(fileStorageService.latestAiAnalysisResultPath(projectId), result);
        Files.createDirectories(fileStorageService.reportsDirectory(projectId));
        Files.writeString(fileStorageService.reportsDirectory(projectId).resolve("ai-analysis.md"), markdown, StandardCharsets.UTF_8);
        return result;
    }

    private LargeModelAnswer tryAnalyzeWithLargeModel(
            String projectId,
            LocAnalysisResult loc,
            ComplexityAnalysisResult complexity,
            ObjectOrientedAnalysisResult oo,
            EstimationResult estimation,
            FunctionPointResult functionPoint,
            UseCasePointResult useCasePoint
    ) {
        if (!isLargeModelConfigured()) {
            log.info("Large model analysis skipped: enabled={}, effectiveEnabled=false, apiKeyConfigured={}, modelConfigured={}, baseUrlConfigured={}",
                    largeModelProperties.isEnabled(),
                    !isBlank(largeModelProperties.getApiKey()),
                    !isBlank(largeModelProperties.getModel()),
                    !isBlank(largeModelProperties.getBaseUrl()));
            return null;
        }
        if (!largeModelProperties.isEnabled()) {
            log.warn("Large model enabled flag is false, but API key/model/baseUrl are configured; proceeding with large model call for experiment validation.");
        }
        try {
            String prompt = buildLargeModelPrompt(projectId, loc, complexity, oo, estimation, functionPoint, useCasePoint);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", largeModelProperties.getModel());
            payload.put("temperature", 0.2);
            payload.put("max_completion_tokens", Math.max(512, largeModelProperties.getMaxCompletionTokens()));
            payload.put("response_format", Map.of("type", "json_object"));
            if (!isBlank(largeModelProperties.getReasoningEffort())) {
                payload.put("reasoning_effort", largeModelProperties.getReasoningEffort());
            }
            payload.put("messages", List.of(
                    Map.of("role", "system", "content", "你是软件质量保证课程实验中的软件度量专家。必须只输出一个合法 JSON 对象，不要输出 Markdown，不要解释，不要代码块。"),
                    Map.of("role", "user", "content", prompt)
            ));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(largeModelProperties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(Math.max(5, largeModelProperties.getTimeoutSeconds())))
                    .header("Authorization", "Bearer " + largeModelProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Large model analysis failed with HTTP status {} and response body: {}", response.statusCode(), truncate(response.body(), 600));
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (isBlank(content)) {
                log.warn("Large model analysis failed: empty assistant content. Raw response: {}", truncate(response.body(), 600));
                return null;
            }
            return parseLargeModelAnswer(content);
        } catch (Exception exception) {
            log.warn("Large model analysis failed and will fallback to local rules: {}", exception.getMessage());
            return null;
        }
    }

    private String buildLargeModelPrompt(
            String projectId,
            LocAnalysisResult loc,
            ComplexityAnalysisResult complexity,
            ObjectOrientedAnalysisResult oo,
            EstimationResult estimation,
            FunctionPointResult functionPoint,
            UseCasePointResult useCasePoint
    ) throws IOException {
        Map<String, Object> summary = Map.of(
                "projectId", projectId,
                "loc", Map.of(
                        "fileCount", loc.summary().fileCount(),
                        "sourceLines", loc.summary().sourceLines(),
                        "commentRate", loc.summary().commentRate()
                ),
                "complexity", Map.of(
                        "methodCount", complexity.summary().methodCount(),
                        "averageComplexity", complexity.summary().averageComplexity(),
                        "maxComplexity", complexity.summary().maxComplexity(),
                        "highRiskMethodCount", complexity.summary().highRiskMethodCount(),
                        "topMethods", topMethods(complexity)
                ),
                "objectOriented", Map.of(
                        "classCount", oo.summary().classCount(),
                        "interfaceCount", oo.summary().interfaceCount(),
                        "averageCbo", oo.summary().averageCbo(),
                        "averageRfc", oo.summary().averageRfc(),
                        "maxDit", oo.summary().maxDit(),
                        "highRiskClassCount", oo.summary().highRiskClassCount(),
                        "topClasses", topClasses(oo)
                ),
                "estimation", Map.of(
                        "kloc", estimation.kloc(),
                        "effortPersonMonths", estimation.effortPersonMonths(),
                        "developmentMonths", estimation.developmentMonths(),
                        "estimatedCost", estimation.estimatedCost()
                ),
                "functionPoint", functionPoint == null ? Map.of("available", false) : Map.of(
                        "available", true,
                        "unadjustedFunctionPoints", functionPoint.unadjustedFunctionPoints(),
                        "adjustedFunctionPoints", functionPoint.adjustedFunctionPoints()
                ),
                "useCasePoint", useCasePoint == null ? Map.of("available", false) : Map.of(
                        "available", true,
                        "useCasePoints", useCasePoint.useCasePoints(),
                        "estimatedPersonMonths", useCasePoint.estimatedPersonMonths()
                )
        );
        return "请根据以下软件度量 JSON 摘要生成质量分析。要求返回严格 JSON，字段为："
                + "overallAssessment 字符串，riskItems 字符串数组，refactoringSuggestions 字符串数组，testSuggestions 字符串数组。"
                + "数组元素必须是字符串，不要使用对象。每个数组保留 3 到 6 条，内容面向 Java 项目质量改进，不要编造源代码细节。\n"
                + objectMapper.writeValueAsString(summary);
    }

    private LargeModelAnswer parseLargeModelAnswer(String content) throws IOException {
        String json = extractJson(content);
        JsonNode root = objectMapper.readTree(json);
        String assessment = root.path("overallAssessment").asText("");
        List<String> risks = readStringList(root.path("riskItems"));
        List<String> refactoring = readStringList(root.path("refactoringSuggestions"));
        List<String> tests = readStringList(root.path("testSuggestions"));
        if (isBlank(assessment) || risks.isEmpty() || refactoring.isEmpty() || tests.isEmpty()) {
            return null;
        }
        return new LargeModelAnswer(
                largeModelProperties.getProvider() + ":" + largeModelProperties.getModel(),
                assessment,
                limit(risks, 8),
                limit(refactoring, 8),
                limit(tests, 8)
        );
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        node.forEach(item -> {
            String value = item.isTextual() ? item.asText("") : summarizeObjectItem(item);
            if (!isBlank(value)) {
                values.add(value);
            }
        });
        return values;
    }

    private String summarizeObjectItem(JsonNode item) {
        if (item == null || item.isNull()) {
            return "";
        }
        String[] preferredFields = {
                "riskDescription",
                "suggestionContent",
                "testContent",
                "description",
                "content",
                "reason",
                "message"
        };
        for (String field : preferredFields) {
            String value = item.path(field).asText("");
            if (!isBlank(value)) {
                String reason = item.path("reason").asText("");
                return isBlank(reason) || value.equals(reason) ? value : value + "（原因：" + reason + "）";
            }
        }
        return item.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isLargeModelConfigured() {
        return !isBlank(largeModelProperties.getApiKey())
                && !isBlank(largeModelProperties.getModel())
                && !isBlank(largeModelProperties.getBaseUrl());
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    public AiAnalysisResult latestResult(String projectId) throws IOException {
        Path latestPath = fileStorageService.latestAiAnalysisResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        return fileStorageService.readJson(latestPath, AiAnalysisResult.class);
    }

    private LocAnalysisResult ensureLoc(String projectId) throws IOException {
        LocAnalysisResult result = locAnalysisService.latestResult(projectId);
        return result == null ? locAnalysisService.analyzeProject(projectId) : result;
    }

    private ComplexityAnalysisResult ensureComplexity(String projectId) throws IOException {
        ComplexityAnalysisResult result = complexityAnalysisService.latestResult(projectId);
        return result == null ? complexityAnalysisService.analyzeProject(projectId) : result;
    }

    private ObjectOrientedAnalysisResult ensureObjectOriented(String projectId) throws IOException {
        ObjectOrientedAnalysisResult result = objectOrientedAnalysisService.latestResult(projectId);
        return result == null ? objectOrientedAnalysisService.analyzeProject(projectId) : result;
    }

    private EstimationResult ensureEstimation(String projectId) throws IOException {
        EstimationResult result = estimationService.latestResult(projectId);
        return result == null ? estimationService.analyzeProject(projectId, null) : result;
    }

    private List<String> buildRisks(LocAnalysisResult loc, ComplexityAnalysisResult complexity, ObjectOrientedAnalysisResult oo) {
        List<String> risks = new ArrayList<>();
        if (loc.summary().commentRate() < 0.1) {
            risks.add("项目注释率低于 10%，复杂业务逻辑和公共接口的可读性风险较高。");
        }
        if (complexity.summary().highRiskMethodCount() > 0) {
            risks.add("存在 " + complexity.summary().highRiskMethodCount() + " 个高风险方法，建议优先检查条件分支和循环嵌套。");
        }
        if (complexity.summary().averageComplexity() > 10) {
            risks.add("平均圈复杂度超过 10，说明整体控制流偏复杂，后续维护和测试成本可能上升。");
        }
        if (oo.summary().highRiskClassCount() > 0) {
            risks.add("存在 " + oo.summary().highRiskClassCount() + " 个高风险类，需要关注耦合、RFC、继承深度、WMC 和 LCOM。");
        }
        if (oo.summary().averageCbo() > 10) {
            risks.add("平均 CBO 超过 10，类之间依赖较密集，变更影响面可能扩大。");
        }
        if (oo.summary().averageRfc() > 15) {
            risks.add("平均 RFC 偏高，说明类的响应集合较大，测试和调试成本会增加。");
        }
        if (risks.isEmpty()) {
            risks.add("当前关键阈值未触发明显高风险项，项目结构整体处于可控状态。");
        }
        return risks;
    }

    private List<String> buildRefactoringSuggestions(ComplexityAnalysisResult complexity, ObjectOrientedAnalysisResult oo) {
        List<String> suggestions = new ArrayList<>();
        topMethods(complexity).forEach(method -> suggestions.add(
                "拆分方法 `" + method.methodName() + "`，优先降低圈复杂度 " + method.cyclomaticComplexity() + " 对测试路径数量的影响。"
        ));
        topClasses(oo).forEach(item -> suggestions.add(
                "检查类 `" + item.className() + "` 的职责边界，当前 CBO=" + item.cbo()
                        + "、RFC=" + item.rfc() + "、WMC=" + item.wmc() + "、LCOM=" + item.lcom() + "。"
        ));
        if (suggestions.isEmpty()) {
            suggestions.add("保持当前模块划分，后续可结合真实需求变更继续观察类规模、耦合和方法复杂度趋势。");
        }
        return limit(suggestions, 8);
    }

    private List<String> buildTestSuggestions(ComplexityAnalysisResult complexity, ObjectOrientedAnalysisResult oo) {
        List<String> suggestions = new ArrayList<>();
        topMethods(complexity).forEach(method -> suggestions.add(
                "为 `" + method.methodName() + "` 补充分支覆盖测试，至少覆盖主要 if/for/while/case 路径。"
        ));
        topClasses(oo).forEach(item -> suggestions.add(
                "为 `" + item.className() + "` 增加集成测试或契约测试，降低高耦合类变更后的回归风险。"
        ));
        suggestions.add("对 LoC、圈复杂度、CK/LK 和 COCOMO 估算接口保留回归用例，确保后续修改不会破坏核心度量结果。");
        return limit(suggestions, 8);
    }

    private String buildAssessment(
            LocAnalysisResult loc,
            ComplexityAnalysisResult complexity,
            ObjectOrientedAnalysisResult oo,
            EstimationResult estimation,
            List<String> risks
    ) {
        return "本项目共分析 " + loc.summary().fileCount() + " 个 Java 文件、"
                + loc.summary().sourceLines() + " 行有效代码、"
                + complexity.summary().methodCount() + " 个可执行方法和 "
                + (oo.summary().classCount() + oo.summary().interfaceCount()) + " 个类/接口。"
                + " 平均圈复杂度为 " + complexity.summary().averageComplexity()
                + "，平均 CBO 为 " + oo.summary().averageCbo()
                + "，平均 RFC 为 " + oo.summary().averageRfc()
                + "，基础 COCOMO 估算工作量为 " + estimation.effortPersonMonths() + " 人月。"
                + " 当前主要关注点为：" + risks.get(0);
    }

    private String buildMarkdown(
            String projectId,
            LocAnalysisResult loc,
            ComplexityAnalysisResult complexity,
            ObjectOrientedAnalysisResult oo,
            EstimationResult estimation,
            String assessment,
            List<String> risks,
            List<String> refactoring,
            List<String> tests,
            String modelName
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 智能质量分析建议\n\n");
        builder.append("## 基本信息\n\n");
        builder.append("- 项目 ID：").append(projectId).append("\n");
        builder.append("- 分析器：").append(modelName).append("\n");
        builder.append("- 说明：优先使用已配置的大模型服务，未配置或调用失败时自动回退到本地规则分析。\n\n");
        builder.append("## 总体评价\n\n").append(assessment).append("\n\n");
        builder.append("## 指标摘要\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| Java 文件数 | ").append(loc.summary().fileCount()).append(" |\n");
        builder.append("| 有效代码行 | ").append(loc.summary().sourceLines()).append(" |\n");
        builder.append("| 平均圈复杂度 | ").append(complexity.summary().averageComplexity()).append(" |\n");
        builder.append("| 高风险方法数 | ").append(complexity.summary().highRiskMethodCount()).append(" |\n");
        builder.append("| 平均 CBO | ").append(oo.summary().averageCbo()).append(" |\n");
        builder.append("| 平均 RFC | ").append(oo.summary().averageRfc()).append(" |\n");
        builder.append("| 高风险类数 | ").append(oo.summary().highRiskClassCount()).append(" |\n");
        builder.append("| 估算工作量 | ").append(estimation.effortPersonMonths()).append(" 人月 |\n\n");
        appendList(builder, "主要风险", risks);
        appendList(builder, "重构建议", refactoring);
        appendList(builder, "测试建议", tests);
        return builder.toString();
    }

    private void appendList(StringBuilder builder, String title, List<String> items) {
        builder.append("## ").append(title).append("\n\n");
        for (String item : items) {
            builder.append("- ").append(item).append("\n");
        }
        builder.append("\n");
    }

    private List<MethodComplexityMetric> topMethods(ComplexityAnalysisResult result) {
        return result.methods().stream()
                .filter(method -> method.cyclomaticComplexity() > 10 || "HIGH".equals(method.riskLevel()))
                .sorted(Comparator.comparingInt(MethodComplexityMetric::cyclomaticComplexity).reversed())
                .limit(5)
                .toList();
    }

    private List<ClassMetric> topClasses(ObjectOrientedAnalysisResult result) {
        return result.classes().stream()
                .filter(item -> "HIGH".equals(item.riskLevel()) || item.cbo() > 10 || item.rfc() > 20 || item.wmc() > 40 || item.lcom() > 20)
                .sorted(Comparator.comparingInt(this::classRiskScore).reversed())
                .limit(5)
                .toList();
    }

    private int classRiskScore(ClassMetric item) {
        return item.cbo() * 3 + item.rfc() + item.wmc() + item.lcom() + item.dit() * 2;
    }

    private List<String> limit(List<String> values, int size) {
        return values.size() <= size ? values : values.subList(0, size);
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-ai-" + timestamp + "-" + suffix;
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private record LargeModelAnswer(
            String modelName,
            String overallAssessment,
            List<String> riskItems,
            List<String> refactoringSuggestions,
            List<String> testSuggestions
    ) {
    }
}
