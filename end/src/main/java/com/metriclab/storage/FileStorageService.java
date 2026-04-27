package com.metriclab.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.metriclab.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class FileStorageService {

    private final FileStorageProperties properties;
    private final ObjectMapper objectMapper;
    private Path rootPath;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void initialize() throws IOException {
        rootPath = Paths.get(properties.getRoot()).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
        Files.createDirectories(rootPath.resolve("config"));
        Files.createDirectories(rootPath.resolve("projects"));
        ensureJsonFile(rootPath.resolve("projects.json"), Map.of("projects", List.of()));
        ensureJsonFile(rootPath.resolve("config").resolve("thresholds.json"), defaultThresholds());
    }

    public Path rootPath() {
        return rootPath;
    }

    public List<String> initializedPaths() {
        return List.of(
                rootPath.toString(),
                rootPath.resolve("config").toString(),
                rootPath.resolve("projects").toString(),
                rootPath.resolve("projects.json").toString(),
                rootPath.resolve("config").resolve("thresholds.json").toString()
        );
    }

    public Path projectsIndexPath() {
        return rootPath.resolve("projects.json");
    }

    public Path thresholdsPath() {
        return rootPath.resolve("config").resolve("thresholds.json");
    }

    public Path projectDirectory(String projectId) {
        return rootPath.resolve("projects").resolve(projectId);
    }

    public void createProjectDirectories(String projectId) throws IOException {
        Path projectPath = projectDirectory(projectId);
        Files.createDirectories(projectPath.resolve("uploads"));
        Files.createDirectories(projectPath.resolve("tasks"));
    }

    public Path uploadsDirectory(String projectId) {
        return projectDirectory(projectId).resolve("uploads");
    }

    public Path uploadsIndexPath(String projectId) {
        return uploadsDirectory(projectId).resolve("uploads.json");
    }

    public Path tasksDirectory(String projectId) {
        return projectDirectory(projectId).resolve("tasks");
    }

    public Path taskDirectory(String projectId, String taskId) {
        return tasksDirectory(projectId).resolve(taskId);
    }

    public Path latestLocResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-loc-result.json");
    }

    public Path latestComplexityResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-complexity-result.json");
    }

    public Path latestObjectOrientedResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-object-oriented-result.json");
    }

    public Path latestEstimationResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-estimation-result.json");
    }

    public Path latestFunctionPointResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-function-point-result.json");
    }

    public Path latestUseCasePointResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-use-case-point-result.json");
    }

    public Path latestModelAnalysisResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-model-analysis-result.json");
    }

    public Path latestAiAnalysisResultPath(String projectId) {
        return tasksDirectory(projectId).resolve("latest-ai-analysis-result.json");
    }

    public Path reportsDirectory(String projectId) {
        return projectDirectory(projectId).resolve("reports");
    }

    public void invalidateDerivedArtifacts(String projectId) throws IOException {
        List<Path> stalePaths = List.of(
                latestLocResultPath(projectId),
                latestComplexityResultPath(projectId),
                latestObjectOrientedResultPath(projectId),
                latestEstimationResultPath(projectId),
                latestModelAnalysisResultPath(projectId),
                latestAiAnalysisResultPath(projectId),
                reportsDirectory(projectId).resolve("loc-report.md"),
                reportsDirectory(projectId).resolve("loc-report-meta.json"),
                reportsDirectory(projectId).resolve("complexity-report.md"),
                reportsDirectory(projectId).resolve("complexity-report-meta.json"),
                reportsDirectory(projectId).resolve("object-oriented-report.md"),
                reportsDirectory(projectId).resolve("object-oriented-report-meta.json"),
                reportsDirectory(projectId).resolve("estimation-report.md"),
                reportsDirectory(projectId).resolve("estimation-report-meta.json"),
                reportsDirectory(projectId).resolve("model-analysis-report.md"),
                reportsDirectory(projectId).resolve("ai-analysis.md"),
                reportsDirectory(projectId).resolve("final-metric-report.md"),
                reportsDirectory(projectId).resolve("metrics.xml")
        );
        for (Path stalePath : stalePaths) {
            Files.deleteIfExists(stalePath);
        }
    }

    public String storeUpload(String projectId, MultipartFile file, String fileId) throws IOException {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String storedName = extension.isEmpty() ? fileId : fileId + "." + extension;
        Path target = uploadsDirectory(projectId).resolve(storedName).normalize();
        if (!target.startsWith(uploadsDirectory(projectId).normalize())) {
            throw new IllegalArgumentException("文件名不合法");
        }
        Files.createDirectories(target.getParent());
        file.transferTo(target);
        return storedName;
    }

    public void deleteProjectDirectory(String projectId) throws IOException {
        deleteRecursively(projectDirectory(projectId));
    }

    public void deleteUpload(String projectId, String storedName) throws IOException {
        Path target = uploadsDirectory(projectId).resolve(storedName).normalize();
        if (!target.startsWith(uploadsDirectory(projectId).normalize())) {
            throw new IllegalArgumentException("文件名不合法");
        }
        Files.deleteIfExists(target);
    }

    private void ensureJsonFile(Path path, Object defaultValue) throws IOException {
        if (Files.notExists(path)) {
            writeJson(path, defaultValue);
        }
    }

    public void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        objectMapper.writeValue(path.toFile(), value);
    }

    public <T> T readJson(Path path, Class<T> type) throws IOException {
        return objectMapper.readValue(path.toFile(), type);
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path current : paths) {
                Files.deleteIfExists(current);
            }
        }
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private Map<String, Object> defaultThresholds() {
        return Map.of(
                "cyclomaticComplexity", Map.of("low", 10, "medium", 20, "high", 50),
                "cbo", Map.of("low", 5, "medium", 10, "high", 15),
                "wmc", Map.of("low", 20, "medium", 40, "high", 60),
                "lcom", Map.of("low", 10, "medium", 20, "high", 40),
                "commentRate", Map.of("low", 0.1, "medium", 0.2, "high", 0.3)
        );
    }
}
