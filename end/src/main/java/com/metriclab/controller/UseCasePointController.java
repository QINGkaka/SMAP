package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.UseCasePointReportResult;
import com.metriclab.model.dto.UseCasePointRequest;
import com.metriclab.model.dto.UseCasePointResult;
import com.metriclab.service.UseCasePointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/use-case-point")
public class UseCasePointController {

    private final UseCasePointService useCasePointService;

    public UseCasePointController(UseCasePointService useCasePointService) {
        this.useCasePointService = useCasePointService;
    }

    @PostMapping("/analyze")
    public ApiResponse<UseCasePointResult> analyze(@PathVariable String projectId, @RequestBody(required = false) UseCasePointRequest request) throws IOException {
        return ApiResponse.ok("用例点估算完成", useCasePointService.analyzeProject(projectId, request));
    }

    @GetMapping("/latest")
    public ApiResponse<UseCasePointResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(useCasePointService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<UseCasePointReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("用例点 Markdown 报告生成成功", useCasePointService.exportMarkdownReport(projectId));
    }
}
