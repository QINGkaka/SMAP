package com.metriclab.service;

import com.metriclab.model.dto.CreateProjectRequest;
import com.metriclab.model.dto.ProjectIndex;
import com.metriclab.model.dto.ProjectInfo;
import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final FileStorageService fileStorageService;

    public ProjectService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public synchronized List<ProjectInfo> listProjects() throws IOException {
        ProjectIndex index = readIndex();
        return index.projects().stream()
                .sorted(Comparator.comparing(ProjectInfo::createdAt).reversed())
                .toList();
    }

    public synchronized ProjectInfo createProject(CreateProjectRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("项目参数不能为空");
        }
        String name = normalizeRequired(request.name(), "项目名称不能为空");
        String language = normalizeOptional(request.language(), "Java");
        String description = normalizeOptional(request.description(), "");
        OffsetDateTime now = OffsetDateTime.now();
        String id = createProjectId(now);
        ProjectInfo project = new ProjectInfo(id, name, language, description, now, now);

        ProjectIndex index = readIndex();
        List<ProjectInfo> projects = new ArrayList<>(index.projects());
        projects.add(project);
        fileStorageService.writeJson(fileStorageService.projectsIndexPath(), new ProjectIndex(projects));
        fileStorageService.createProjectDirectories(id);
        fileStorageService.writeJson(fileStorageService.projectDirectory(id).resolve("project.json"), project);
        return project;
    }

    public synchronized void deleteProject(String projectId) throws IOException {
        ProjectIndex index = readIndex();
        boolean exists = index.projects().stream().anyMatch(project -> project.id().equals(projectId));
        if (!exists) {
            throw new IllegalArgumentException("项目不存在：" + projectId);
        }
        List<ProjectInfo> projects = index.projects().stream()
                .filter(project -> !project.id().equals(projectId))
                .toList();
        fileStorageService.writeJson(fileStorageService.projectsIndexPath(), new ProjectIndex(projects));
        fileStorageService.deleteProjectDirectory(projectId);
    }

    private ProjectIndex readIndex() throws IOException {
        ProjectIndex index = fileStorageService.readJson(fileStorageService.projectsIndexPath(), ProjectIndex.class);
        if (index.projects() == null) {
            return new ProjectIndex(List.of());
        }
        return index;
    }

    private String createProjectId(OffsetDateTime now) {
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "project-" + timestamp + "-" + suffix;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value, String defaultValue) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? defaultValue : normalized;
    }
}
