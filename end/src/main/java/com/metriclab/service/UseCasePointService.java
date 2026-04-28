package com.metriclab.service;

import com.metriclab.model.dto.UseCasePointReportResult;
import com.metriclab.model.dto.UseCasePointRequest;
import com.metriclab.model.dto.UseCasePointResult;
import com.metriclab.model.dto.UploadedFileInfo;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UseCasePointService {

    private static final double DEFAULT_PRODUCTIVITY_HOURS = 28.0;
    private static final double HOURS_PER_PERSON_MONTH = 160.0;
    private static final double[] TECHNICAL_WEIGHTS = {2, 1, 1, 1, 1, 0.5, 0.5, 2, 1, 1, 1, 1, 1};
    private static final double[] ENVIRONMENTAL_WEIGHTS = {1.5, 0.5, 1, 0.5, 1, 2, -1, -1};
    private static final double MAX_TECHNICAL_FACTOR_TOTAL = 65.0;
    private static final double MIN_ENVIRONMENTAL_FACTOR_TOTAL = -10.0;
    private static final double MAX_ENVIRONMENTAL_FACTOR_TOTAL = 40.0;

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public UseCasePointService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized UseCasePointResult analyzeProject(String projectId, UseCasePointRequest request) throws IOException {
        uploadService.listFiles(projectId);
        UseCasePointRequest safeRequest = sanitizeRequest(projectId, request == null ? inferRequest(projectId) : request);
        int actorWeight = nonNegative(safeRequest.simpleActors()) * 1
                + nonNegative(safeRequest.averageActors()) * 2
                + nonNegative(safeRequest.complexActors()) * 3;
        int useCaseWeight = nonNegative(safeRequest.simpleUseCases()) * 5
                + nonNegative(safeRequest.averageUseCases()) * 10
                + nonNegative(safeRequest.complexUseCases()) * 15;
        int uucp = actorWeight + useCaseWeight;
        double technicalTotal = resolveWeightedTotal(safeRequest.technicalFactors(), safeRequest.technicalFactorTotal(), TECHNICAL_WEIGHTS, 30.0);
        double environmentalTotal = resolveWeightedTotal(safeRequest.environmentalFactors(), safeRequest.environmentalFactorTotal(), ENVIRONMENTAL_WEIGHTS, 20.0);
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
                round2(technicalTotal),
                tcf,
                round2(environmentalTotal),
                ecf,
                ucp,
                productivity,
                hours,
                personMonths,
                now
        );
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "USE_CASE_POINT", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("use-case-point-request.json"), safeRequest);
        fileStorageService.writeJson(taskDirectory.resolve("use-case-point-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestUseCasePointRequestPath(projectId), safeRequest);
        fileStorageService.writeJson(fileStorageService.latestUseCasePointResultPath(projectId), result);
        return result;
    }

    public UseCasePointRequest draftRequest(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestRequestPath = fileStorageService.latestUseCasePointRequestPath(projectId);
        if (fileStorageService.exists(latestRequestPath)) {
            return sanitizeRequest(projectId, fileStorageService.readJson(latestRequestPath, UseCasePointRequest.class));
        }
        return inferRequest(projectId);
    }

    public UseCasePointRequest assistRequest(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        return inferRequest(projectId);
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
        return new UseCasePointRequest(1, 1, 0, 2, 1, 0, 30.0, 20.0, DEFAULT_PRODUCTIVITY_HOURS, List.of(), List.of());
    }

    private UseCasePointRequest sanitizeRequest(String projectId, UseCasePointRequest request) throws IOException {
        if (request == null) {
            return inferRequest(projectId);
        }
        return new UseCasePointRequest(
                nonNegative(request.simpleActors()),
                nonNegative(request.averageActors()),
                nonNegative(request.complexActors()),
                nonNegative(request.simpleUseCases()),
                nonNegative(request.averageUseCases()),
                nonNegative(request.complexUseCases()),
                request.technicalFactorTotal(),
                request.environmentalFactorTotal(),
                request.productivityHoursPerUseCasePoint(),
                sanitizeScores(request.technicalFactors(), TECHNICAL_WEIGHTS.length),
                sanitizeScores(request.environmentalFactors(), ENVIRONMENTAL_WEIGHTS.length)
        );
    }

    private List<Integer> sanitizeScores(List<Integer> values, int expectedSize) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Integer> sanitized = new ArrayList<>();
        for (int index = 0; index < Math.min(values.size(), expectedSize); index++) {
            Integer value = values.get(index);
            sanitized.add(clamp(value == null ? 0 : value, 0, 5));
        }
        return List.copyOf(sanitized);
    }

    private UseCasePointRequest inferRequest(String projectId) throws IOException {
        List<UploadedFileInfo> files = uploadService.listFiles(projectId);
        UseCaseInference inference = inferFromModelFiles(projectId, files);
        if (!inference.hasSignal()) {
            inference = inferFromProjectStructure(files);
        }
        if (!inference.hasSignal()) {
            return defaultRequest();
        }
        return new UseCasePointRequest(
                inference.simpleActors,
                inference.averageActors,
                inference.complexActors,
                inference.simpleUseCases,
                inference.averageUseCases,
                inference.complexUseCases,
                30.0,
                20.0,
                DEFAULT_PRODUCTIVITY_HOURS,
                defaultTechnicalFactors(),
                defaultEnvironmentalFactors()
        );
    }

    private UseCaseInference inferFromModelFiles(String projectId, List<UploadedFileInfo> files) throws IOException {
        UseCaseInference inference = new UseCaseInference();
        for (UploadedFileInfo file : files) {
            if (!Set.of("xml", "xmi", "oom").contains(file.fileType())) {
                continue;
            }
            Path path = fileStorageService.uploadsDirectory(projectId).resolve(file.storedName());
            try {
                parseUseCaseModel(path, inference);
            } catch (ParserConfigurationException | SAXException ignored) {
                // Skip non-UML XML payloads without failing the project.
            }
        }
        inference.normalize();
        return inference;
    }

    private void parseUseCaseModel(Path path, UseCaseInference inference)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document document = factory.newDocumentBuilder().parse(path.toFile());
        document.getDocumentElement().normalize();
        NodeList nodes = document.getDocumentElement().getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (!(node instanceof Element element)) {
                continue;
            }
            String tagName = localName(element);
            String type = firstNonBlank(attribute(element, "xmi:type"), attribute(element, "type"));
            String name = firstNonBlank(attribute(element, "name"), attribute(element, "Name"), "");
            if (isActorElement(tagName, type)) {
                inference.addActor(name);
            } else if (isUseCaseElement(tagName, type)) {
                inference.addUseCase(name);
            }
        }
    }

    private UseCaseInference inferFromProjectStructure(List<UploadedFileInfo> files) {
        UseCaseInference inference = new UseCaseInference();
        for (UploadedFileInfo file : files) {
            String name = file.originalName().toLowerCase(Locale.ROOT);
            if ("java".equals(file.fileType())) {
                if (name.contains("controller")) {
                    inference.addActor(name);
                    inferUseCaseFromName(name, inference);
                } else if (name.contains("service")) {
                    inferUseCaseFromName(name, inference);
                } else if (name.contains("client") || name.contains("gateway") || name.contains("remote")) {
                    inference.addActor(name);
                } else if (containsAny(name, "register", "course", "student", "review", "approve", "enroll")) {
                    inferUseCaseFromName(name, inference);
                }
            }
        }
        inference.normalize();
        return inference;
    }

    private void inferUseCaseFromName(String name, UseCaseInference inference) {
        if (containsAny(name, "report", "export", "approve", "review", "audit", "analy")) {
            inference.complexUseCases++;
        } else if (containsAny(name, "manage", "assign", "register", "update", "delete", "enroll")) {
            inference.averageUseCases++;
        } else {
            inference.simpleUseCases++;
        }
    }

    private List<Integer> defaultTechnicalFactors() {
        return List.of(2, 3, 2, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3);
    }

    private List<Integer> defaultEnvironmentalFactors() {
        return List.of(3, 2, 2, 3, 3, 2, 2, 3);
    }

    private boolean isActorElement(String tagName, String type) {
        return tagName.equalsIgnoreCase("actor")
                || containsIgnoreCase(type, "Actor")
                || tagName.equalsIgnoreCase("packagedElement") && containsIgnoreCase(type, "Actor");
    }

    private boolean isUseCaseElement(String tagName, String type) {
        return tagName.equalsIgnoreCase("usecase")
                || tagName.equalsIgnoreCase("useCase")
                || containsIgnoreCase(type, "UseCase")
                || tagName.equalsIgnoreCase("packagedElement") && containsIgnoreCase(type, "UseCase");
    }

    private String attribute(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    private String localName(Element element) {
        String tagName = element.getTagName();
        int colonIndex = tagName.indexOf(':');
        return colonIndex >= 0 ? tagName.substring(colonIndex + 1) : tagName;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }

    private int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    private double resolveWeightedTotal(List<Integer> values, Number fallback, double[] weights, double defaultValue) {
        if (values != null && !values.isEmpty()) {
            double total = 0;
            for (int index = 0; index < Math.min(values.size(), weights.length); index++) {
                total += clamp(values.get(index) == null ? 0 : values.get(index), 0, 5) * weights[index];
            }
            return total;
        }
        return validateFallbackTotal(fallback, weights, defaultValue);
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

    private double validateFallbackTotal(Number fallback, double[] weights, double defaultValue) {
        double value = fallback == null ? defaultValue : fallback.doubleValue();
        if (weights == TECHNICAL_WEIGHTS) {
            if (value < 0 || value > MAX_TECHNICAL_FACTOR_TOTAL) {
                throw new IllegalArgumentException("鎶€鏈洜瀛愬姞鏉冩€诲垎蹇呴』鍦?0 鍒?65 涔嬮棿");
            }
            return value;
        }
        if (value < MIN_ENVIRONMENTAL_FACTOR_TOTAL || value > MAX_ENVIRONMENTAL_FACTOR_TOTAL) {
            throw new IllegalArgumentException("鐜鍥犲瓙鍔犳潈鎬诲垎蹇呴』鍦?10 鍒?40 涔嬮棿");
        }
        return value;
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
        builder.append("| 技术因子加权总分 TFactor | ").append(result.technicalFactorTotal()).append(" |\n");
        builder.append("| 技术复杂度因子 TCF | ").append(result.technicalComplexityFactor()).append(" |\n");
        builder.append("| 环境因子加权总分 EFactor | ").append(result.environmentalFactorTotal()).append(" |\n");
        builder.append("| 环境复杂度因子 ECF | ").append(result.environmentalComplexityFactor()).append(" |\n");
        builder.append("| 用例点 UCP | ").append(result.useCasePoints()).append(" |\n");
        builder.append("| 生产率 | ").append(result.productivityHoursPerUseCasePoint()).append(" 小时/用例点 |\n");
        builder.append("| 估算工时 | ").append(result.estimatedHours()).append(" 小时 |\n");
        builder.append("| 估算人月 | ").append(result.estimatedPersonMonths()).append(" 人月 |\n");
        return builder.toString();
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private static final class UseCaseInference {
        private int simpleActors;
        private int averageActors;
        private int complexActors;
        private int simpleUseCases;
        private int averageUseCases;
        private int complexUseCases;

        private void addActor(String name) {
            String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
            if (normalized.contains("system") || normalized.contains("service") || normalized.contains("platform")) {
                complexActors++;
            } else if (normalized.contains("admin") || normalized.contains("teacher") || normalized.contains("manager")) {
                averageActors++;
            } else {
                simpleActors++;
            }
        }

        private void addUseCase(String name) {
            String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
            if (normalized.contains("report") || normalized.contains("export") || normalized.contains("analy") || normalized.contains("audit")) {
                complexUseCases++;
            } else if (normalized.contains("manage") || normalized.contains("assign") || normalized.contains("review")
                    || normalized.contains("approve") || normalized.contains("update")) {
                averageUseCases++;
            } else {
                simpleUseCases++;
            }
        }

        private boolean hasSignal() {
            return simpleActors + averageActors + complexActors + simpleUseCases + averageUseCases + complexUseCases > 0;
        }

        private void normalize() {
            if (simpleActors + averageActors + complexActors == 0 && simpleUseCases + averageUseCases + complexUseCases > 0) {
                averageActors = 1;
            }
            if (simpleUseCases + averageUseCases + complexUseCases == 0 && simpleActors + averageActors + complexActors > 0) {
                averageUseCases = 1;
            }
        }
    }
}
