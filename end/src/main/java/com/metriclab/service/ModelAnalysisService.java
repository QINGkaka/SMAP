package com.metriclab.service;

import com.metriclab.model.dto.AnalysisScopeRequest;
import com.metriclab.model.dto.ModelAnalysisReportResult;
import com.metriclab.model.dto.ModelAnalysisResult;
import com.metriclab.model.dto.ModelAnalysisSummary;
import com.metriclab.model.dto.ModelClassMetric;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ModelAnalysisService {

    private final FileStorageService fileStorageService;
    private final UploadService uploadService;

    public ModelAnalysisService(FileStorageService fileStorageService, UploadService uploadService) {
        this.fileStorageService = fileStorageService;
        this.uploadService = uploadService;
    }

    public synchronized ModelAnalysisResult analyzeProject(String projectId) throws IOException {
        return analyzeProject(projectId, null);
    }

    public synchronized ModelAnalysisResult analyzeProject(String projectId, AnalysisScopeRequest request) throws IOException {
        List<UploadedFileInfo> files = resolveTargetFiles(projectId, request);
        List<String> analyzedFileIds = resolvedFileIds(files, request);
        Map<String, MutableModelClass> classes = new LinkedHashMap<>();
        int modelFileCount = 0;

        for (UploadedFileInfo file : files) {
            if (!isModelFile(file)) {
                continue;
            }
            modelFileCount++;
            Path path = fileStorageService.uploadsDirectory(projectId).resolve(file.storedName());
            try {
                parseModelFile(path, file.originalName(), classes);
            } catch (ParserConfigurationException | SAXException ignored) {
                // Invalid or non-UML XML model files are skipped so one bad model file does not block the project.
            }
        }

        if (modelFileCount == 0) {
            throw new IllegalArgumentException("当前项目没有可分析的模型文件，请上传 .xml、.xmi 或 .oom 文件");
        }
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("已找到模型文件，但未解析到 UML 类、接口、属性或操作信息");
        }

        applyInheritance(classes);
        List<ModelClassMetric> classMetrics = classes.values().stream()
                .map(MutableModelClass::toMetric)
                .sorted(Comparator.comparing(ModelClassMetric::className))
                .toList();
        ModelAnalysisSummary summary = summarize(modelFileCount, classMetrics);

        OffsetDateTime now = OffsetDateTime.now();
        String taskId = createTaskId(now);
        ModelAnalysisResult result = new ModelAnalysisResult(taskId, projectId, summary, classMetrics, now, analyzedFileIds);
        Path taskDirectory = fileStorageService.taskDirectory(projectId, taskId);
        fileStorageService.writeJson(taskDirectory.resolve("task.json"), new TaskFile(taskId, projectId, "MODEL_ANALYSIS", "FINISHED", now));
        fileStorageService.writeJson(taskDirectory.resolve("model-analysis-result.json"), result);
        fileStorageService.writeJson(fileStorageService.latestModelAnalysisResultPath(projectId), result);
        return result;
    }

    public ModelAnalysisResult latestResult(String projectId) throws IOException {
        uploadService.listFiles(projectId);
        Path latestPath = fileStorageService.latestModelAnalysisResultPath(projectId);
        if (!fileStorageService.exists(latestPath)) {
            return null;
        }
        try {
            return fileStorageService.readJson(latestPath, ModelAnalysisResult.class);
        } catch (IOException exception) {
            return null;
        }
    }

    public ModelAnalysisReportResult exportMarkdownReport(String projectId) throws IOException {
        ModelAnalysisResult result = latestResult(projectId);
        if (result == null) {
            throw new IllegalArgumentException("当前项目还没有模型分析结果，请先执行模型文件度量");
        }
        String content = buildMarkdown(result);
        Path reportPath = fileStorageService.reportsDirectory(projectId).resolve("model-analysis-report.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, content, StandardCharsets.UTF_8);
        return new ModelAnalysisReportResult(projectId, result.taskId(), reportPath.toString(), content);
    }

    private boolean isModelFile(UploadedFileInfo file) {
        return Set.of("xml", "xmi", "oom").contains(file.fileType());
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
                .filter(this::isModelFile)
                .toList();
        if (analyzableFiles.isEmpty()) {
            throw new IllegalArgumentException("当前选择中没有可分析的模型文件，请选择 .xml、.xmi 或 .oom 文件");
        }
        return analyzableFiles;
    }

    private void parseModelFile(Path path, String sourceName, Map<String, MutableModelClass> classes)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Document document = factory.newDocumentBuilder().parse(path.toFile());
        document.getDocumentElement().normalize();
        collectClassElements(document.getDocumentElement(), sourceName, classes);
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

    private void collectClassElements(Element root, String sourceName, Map<String, MutableModelClass> classes) {
        NodeList nodes = root.getElementsByTagName("*");
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (!(node instanceof Element element) || !isClassElement(element)) {
                continue;
            }
            String id = firstNonBlank(attribute(element, "xmi:id"), attribute(element, "id"), attribute(element, "Id"), attribute(element, "name"));
            String name = firstNonBlank(attribute(element, "name"), id);
            if (isBlank(name)) {
                continue;
            }
            MutableModelClass item = classes.computeIfAbsent(id, key -> new MutableModelClass(id, name, sourceName));
            item.className = name;
            item.type = isInterfaceElement(element) ? "INTERFACE" : "CLASS";
            item.sourceUploadName = sourceName;
            item.attributeCount += countChildren(element, Set.of("ownedAttribute", "attribute", "Attribute"));
            item.operationCount += countChildren(element, Set.of("ownedOperation", "operation", "Operation"));
            collectParent(element, item);
        }
    }

    private boolean isClassElement(Element element) {
        String tag = localName(element);
        String type = firstNonBlank(attribute(element, "xmi:type"), attribute(element, "type"));
        return tag.equalsIgnoreCase("class")
                || tag.equalsIgnoreCase("interface")
                || tag.equalsIgnoreCase("packagedElement") && (containsIgnoreCase(type, "Class") || containsIgnoreCase(type, "Interface"))
                || tag.equalsIgnoreCase("ownedMember") && (containsIgnoreCase(type, "Class") || containsIgnoreCase(type, "Interface"));
    }

    private boolean isInterfaceElement(Element element) {
        String tag = localName(element);
        String type = firstNonBlank(attribute(element, "xmi:type"), attribute(element, "type"));
        return tag.equalsIgnoreCase("interface") || containsIgnoreCase(type, "Interface");
    }

    private int countChildren(Element element, Set<String> names) {
        int count = 0;
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement && names.contains(localName(childElement))) {
                count++;
            }
        }
        return count;
    }

    private void collectParent(Element element, MutableModelClass item) {
        String parent = firstNonBlank(attribute(element, "general"), attribute(element, "parent"), attribute(element, "superClass"));
        if (!isBlank(parent)) {
            item.parentRef = cleanReference(parent);
        }
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element childElement && localName(childElement).equalsIgnoreCase("generalization")) {
                String general = cleanReference(firstNonBlank(attribute(childElement, "general"), attribute(childElement, "target")));
                if (!isBlank(general)) {
                    item.parentRef = general;
                }
            }
        }
    }

    private void applyInheritance(Map<String, MutableModelClass> classes) {
        Map<String, MutableModelClass> byName = new HashMap<>();
        classes.values().forEach(item -> byName.put(item.className, item));
        for (MutableModelClass item : classes.values()) {
            MutableModelClass parent = resolveParent(item, classes, byName);
            if (parent != null) {
                item.parentName = parent.className;
                parent.childCount++;
            }
        }
        for (MutableModelClass item : classes.values()) {
            item.inheritanceDepth = inheritanceDepth(item, classes, byName, new HashSet<>());
        }
    }

    private MutableModelClass resolveParent(MutableModelClass item, Map<String, MutableModelClass> byId, Map<String, MutableModelClass> byName) {
        if (isBlank(item.parentRef)) {
            return null;
        }
        return firstNonNull(byId.get(item.parentRef), byName.get(item.parentRef));
    }

    private int inheritanceDepth(MutableModelClass item, Map<String, MutableModelClass> byId, Map<String, MutableModelClass> byName, Set<String> seen) {
        if (isBlank(item.parentRef) || seen.contains(item.id)) {
            return 0;
        }
        seen.add(item.id);
        MutableModelClass parent = resolveParent(item, byId, byName);
        if (parent == null) {
            return 1;
        }
        return 1 + inheritanceDepth(parent, byId, byName, seen);
    }

    private ModelAnalysisSummary summarize(int modelFileCount, List<ModelClassMetric> classes) {
        int classCount = (int) classes.stream().filter(item -> "CLASS".equals(item.type())).count();
        int interfaceCount = (int) classes.stream().filter(item -> "INTERFACE".equals(item.type())).count();
        int attributes = classes.stream().mapToInt(ModelClassMetric::attributeCount).sum();
        int operations = classes.stream().mapToInt(ModelClassMetric::operationCount).sum();
        int inheritance = (int) classes.stream().filter(item -> !isBlank(item.parentName())).count();
        int highRisk = (int) classes.stream().filter(item -> "HIGH".equals(item.riskLevel())).count();
        return new ModelAnalysisSummary(modelFileCount, classCount, interfaceCount, attributes, operations, inheritance, highRisk);
    }

    private String buildMarkdown(ModelAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 模型文件度量报告\n\n");
        builder.append("## 汇总结果\n\n");
        builder.append("| 指标 | 数值 |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| 模型文件数 | ").append(result.summary().fileCount()).append(" |\n");
        builder.append("| 类数量 | ").append(result.summary().classCount()).append(" |\n");
        builder.append("| 接口数量 | ").append(result.summary().interfaceCount()).append(" |\n");
        builder.append("| 属性数量 | ").append(result.summary().attributeCount()).append(" |\n");
        builder.append("| 操作数量 | ").append(result.summary().operationCount()).append(" |\n");
        builder.append("| 继承关系数量 | ").append(result.summary().inheritanceRelationCount()).append(" |\n");
        builder.append("| 高风险类数量 | ").append(result.summary().highRiskClassCount()).append(" |\n\n");
        builder.append("## 类级明细\n\n");
        builder.append("| 类/接口 | 类型 | 属性 | 操作 | 子类 | 继承深度 | 父类 | 风险 |\n");
        builder.append("| --- | --- | ---: | ---: | ---: | ---: | --- | --- |\n");
        for (ModelClassMetric item : result.classes()) {
            builder.append("| ").append(item.className()).append(" | ")
                    .append(item.type()).append(" | ")
                    .append(item.attributeCount()).append(" | ")
                    .append(item.operationCount()).append(" | ")
                    .append(item.childCount()).append(" | ")
                    .append(item.inheritanceDepth()).append(" | ")
                    .append(isBlank(item.parentName()) ? "-" : item.parentName()).append(" | ")
                    .append(item.riskLevel()).append(" |\n");
        }
        return builder.toString();
    }

    private String createTaskId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "task-model-" + timestamp + "-" + suffix;
    }

    private String localName(Element element) {
        String localName = element.getLocalName();
        String name = localName == null ? element.getTagName() : localName;
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private String attribute(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    private String cleanReference(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        int hash = trimmed.lastIndexOf('#');
        return hash >= 0 ? trimmed.substring(hash + 1) : trimmed;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private <T> T firstNonNull(T left, T right) {
        return left == null ? right : left;
    }

    private boolean containsIgnoreCase(String value, String part) {
        return value != null && value.toLowerCase().contains(part.toLowerCase());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TaskFile(String taskId, String projectId, String type, String status, OffsetDateTime createdAt) {
    }

    private static class MutableModelClass {
        private final String id;
        private String className;
        private String type = "CLASS";
        private String sourceUploadName;
        private int attributeCount;
        private int operationCount;
        private int childCount;
        private int inheritanceDepth;
        private String parentRef;
        private String parentName;

        private MutableModelClass(String id, String className, String sourceUploadName) {
            this.id = id;
            this.className = className;
            this.sourceUploadName = sourceUploadName;
        }

        private ModelClassMetric toMetric() {
            int classSize = attributeCount + operationCount;
            String risk = classSize >= 25 || inheritanceDepth >= 4 || childCount >= 6 ? "HIGH"
                    : classSize >= 12 || inheritanceDepth >= 2 || childCount >= 3 ? "MEDIUM"
                    : "LOW";
            return new ModelClassMetric(
                    className,
                    type,
                    sourceUploadName,
                    attributeCount,
                    operationCount,
                    childCount,
                    inheritanceDepth,
                    classSize,
                    parentName,
                    risk
            );
        }
    }
}
