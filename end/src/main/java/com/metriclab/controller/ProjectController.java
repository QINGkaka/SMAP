package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.CreateProjectRequest;
import com.metriclab.model.dto.ProjectInfo;
import com.metriclab.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<List<ProjectInfo>> listProjects() throws IOException {
        return ApiResponse.ok(projectService.listProjects());
    }

    @PostMapping
    public ApiResponse<ProjectInfo> createProject(@RequestBody CreateProjectRequest request) throws IOException {
        return ApiResponse.ok("项目创建成功", projectService.createProject(request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(@PathVariable String projectId) throws IOException {
        projectService.deleteProject(projectId);
        return ApiResponse.ok("项目删除成功", null);
    }
}
