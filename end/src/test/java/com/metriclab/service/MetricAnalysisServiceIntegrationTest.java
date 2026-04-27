package com.metriclab.service;

import com.metriclab.model.dto.AnalysisScopeRequest;
import com.metriclab.model.dto.ComplexityAnalysisResult;
import com.metriclab.model.dto.CreateProjectRequest;
import com.metriclab.model.dto.ModelAnalysisResult;
import com.metriclab.model.dto.ObjectOrientedAnalysisResult;
import com.metriclab.model.dto.ProjectInfo;
import com.metriclab.model.dto.UploadedFileInfo;
import com.metriclab.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "metric.storage.root=target/test-metric-data")
class MetricAnalysisServiceIntegrationTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-metric-data");
    private static final Path JAVA_SAMPLE_ROOT = Path.of("../code/java-metric-sample/src/main/java/sample/metrics");
    private static final Path MODEL_SAMPLE_ROOT = Path.of("../code/model-files");

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private ComplexityAnalysisService complexityAnalysisService;

    @Autowired
    private ObjectOrientedAnalysisService objectOrientedAnalysisService;

    @Autowired
    private ModelAnalysisService modelAnalysisService;

    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void resetStorage() throws IOException {
        if (Files.notExists(STORAGE_ROOT)) {
            return;
        }
        try (var stream = Files.walk(STORAGE_ROOT)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        fileStorageService.initialize();
    }

    @Test
    void supportsWholeProjectAnalysis() throws Exception {
        ProjectInfo project = createProjectWithSamples();

        ComplexityAnalysisResult complexity = complexityAnalysisService.analyzeProject(project.id());
        ObjectOrientedAnalysisResult objectOriented = objectOrientedAnalysisService.analyzeProject(project.id());
        ModelAnalysisResult model = modelAnalysisService.analyzeProject(project.id());

        assertNotNull(complexity);
        assertNotNull(objectOriented);
        assertNotNull(model);
        assertTrue(complexity.summary().fileCount() > 0);
        assertTrue(objectOriented.summary().classCount() + objectOriented.summary().interfaceCount() > 0);
        assertTrue(model.summary().fileCount() > 0);
        assertNotNull(complexityAnalysisService.latestResult(project.id()));
        assertNotNull(objectOrientedAnalysisService.latestResult(project.id()));
        assertNotNull(modelAnalysisService.latestResult(project.id()));

        Map<String, Integer> complexityByMethod = complexity.methods().stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.fileName() + "#" + item.methodName(),
                        item -> item.cyclomaticComplexity()
                ));
        assertEquals(5, complexityByMethod.get("EnrollmentService.java#determineRisk"));
        assertEquals(4, complexityByMethod.get("EnrollmentService.java#enroll"));
        assertEquals(2, complexityByMethod.get("Student.java#isAtRisk"));

        Map<String, com.metriclab.model.dto.ClassMetric> classMetrics = objectOriented.classes().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.metriclab.model.dto.ClassMetric::className,
                        item -> item
                ));
        assertEquals(6, classMetrics.get("Student").rfc());
        assertEquals(5, classMetrics.get("GraduateStudent").rfc());
        assertEquals(0, classMetrics.get("GraduateStudent").cbo());
        assertEquals(4, classMetrics.get("EnrollmentService").cbo());
        assertEquals(12, classMetrics.get("EnrollmentService").rfc());
        assertEquals(2, classMetrics.get("SplitStorage").lcom());
    }

    @Test
    void supportsSelectedFileAnalysis() throws Exception {
        ProjectInfo project = createProjectWithSamples();
        List<UploadedFileInfo> files = uploadService.listFiles(project.id());

        UploadedFileInfo enrollmentService = files.stream()
                .filter(file -> file.originalName().equals("EnrollmentService.java"))
                .findFirst()
                .orElseThrow();
        UploadedFileInfo modelXml = files.stream()
                .filter(file -> file.originalName().equals("academic-model.xml"))
                .findFirst()
                .orElseThrow();

        ComplexityAnalysisResult complexity = complexityAnalysisService.analyzeProject(
                project.id(),
                new AnalysisScopeRequest(List.of(enrollmentService.id()))
        );
        ObjectOrientedAnalysisResult objectOriented = objectOrientedAnalysisService.analyzeProject(
                project.id(),
                new AnalysisScopeRequest(List.of(enrollmentService.id()))
        );
        ModelAnalysisResult model = modelAnalysisService.analyzeProject(
                project.id(),
                new AnalysisScopeRequest(List.of(modelXml.id()))
        );

        assertEquals(1, complexity.summary().fileCount());
        assertEquals(List.of(enrollmentService.id()), complexity.analyzedFileIds());
        assertTrue(objectOriented.classes().stream().allMatch(item -> "EnrollmentService.java".equals(item.fileName())));
        assertEquals(List.of(enrollmentService.id()), objectOriented.analyzedFileIds());
        assertEquals(1, model.summary().fileCount());
        assertTrue(model.classes().stream().allMatch(item -> "academic-model.xml".equals(item.sourceUploadName())));
        assertEquals(List.of(modelXml.id()), model.analyzedFileIds());
    }

    @Test
    void invalidatesUploadDependentLatestResultsWhenProjectFilesChange() throws Exception {
        ProjectInfo project = createProjectWithSamples();

        complexityAnalysisService.analyzeProject(project.id());
        objectOrientedAnalysisService.analyzeProject(project.id());
        modelAnalysisService.analyzeProject(project.id());

        assertNotNull(complexityAnalysisService.latestResult(project.id()));
        assertNotNull(objectOrientedAnalysisService.latestResult(project.id()));
        assertNotNull(modelAnalysisService.latestResult(project.id()));

        uploadService.uploadFile(
                project.id(),
                new MockMultipartFile(
                        "file",
                        "ExtraSample.java",
                        "text/x-java-source",
                        "class ExtraSample { void ping() { if (true) { } } }".getBytes()
                )
        );

        assertNull(complexityAnalysisService.latestResult(project.id()));
        assertNull(objectOrientedAnalysisService.latestResult(project.id()));
        assertNull(modelAnalysisService.latestResult(project.id()));
    }

    @Test
    void fullProjectAnalysisRemainsRecoverableAfterSelectedFileRun() throws Exception {
        ProjectInfo project = createProjectWithSamples();
        ComplexityAnalysisResult fullComplexity = complexityAnalysisService.analyzeProject(project.id());
        ObjectOrientedAnalysisResult fullObjectOriented = objectOrientedAnalysisService.analyzeProject(project.id());

        List<UploadedFileInfo> files = uploadService.listFiles(project.id());
        UploadedFileInfo enrollmentService = files.stream()
                .filter(file -> file.originalName().equals("EnrollmentService.java"))
                .findFirst()
                .orElseThrow();

        complexityAnalysisService.analyzeProject(project.id(), new AnalysisScopeRequest(List.of(enrollmentService.id())));
        objectOrientedAnalysisService.analyzeProject(project.id(), new AnalysisScopeRequest(List.of(enrollmentService.id())));

        ComplexityAnalysisResult rerunComplexity = complexityAnalysisService.analyzeProject(project.id());
        ObjectOrientedAnalysisResult rerunObjectOriented = objectOrientedAnalysisService.analyzeProject(project.id());

        assertTrue(fullComplexity.analyzedFileIds().isEmpty());
        assertTrue(fullObjectOriented.analyzedFileIds().isEmpty());
        assertTrue(rerunComplexity.analyzedFileIds().isEmpty());
        assertTrue(rerunObjectOriented.analyzedFileIds().isEmpty());
        assertEquals(fullComplexity.summary().fileCount(), rerunComplexity.summary().fileCount());
        assertEquals(fullObjectOriented.summary().classCount(), rerunObjectOriented.summary().classCount());
        assertFalse(rerunComplexity.methods().isEmpty());
        assertFalse(rerunObjectOriented.classes().isEmpty());
    }

    private ProjectInfo createProjectWithSamples() throws Exception {
        ProjectInfo project = projectService.createProject(new CreateProjectRequest("integration-test", "Java", "service smoke test"));
        List<Path> javaFiles;
        try (var stream = Files.list(JAVA_SAMPLE_ROOT)) {
            javaFiles = stream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".java"))
                    .sorted()
                    .toList();
        }
        for (Path path : javaFiles) {
            uploadService.uploadFile(project.id(), toMultipartFile(path, "text/x-java-source"));
        }
        List<Path> modelFiles;
        try (var stream = Files.list(MODEL_SAMPLE_ROOT)) {
            modelFiles = stream
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".xml") || fileName.endsWith(".xmi") || fileName.endsWith(".oom");
                    })
                    .sorted()
                    .toList();
        }
        for (Path path : modelFiles) {
            uploadService.uploadFile(project.id(), toMultipartFile(path, "application/xml"));
        }
        return project;
    }

    private MockMultipartFile toMultipartFile(Path path, String contentType) throws IOException {
        return new MockMultipartFile(
                "file",
                path.getFileName().toString(),
                contentType,
                Files.readAllBytes(path)
        );
    }
}
