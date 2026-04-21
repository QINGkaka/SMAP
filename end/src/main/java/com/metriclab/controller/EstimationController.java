package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.EstimationReportResult;
import com.metriclab.model.dto.EstimationRequest;
import com.metriclab.model.dto.EstimationResult;
import com.metriclab.service.EstimationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/estimation")
public class EstimationController {

    private final EstimationService estimationService;

    public EstimationController(EstimationService estimationService) {
        this.estimationService = estimationService;
    }

    @PostMapping("/analyze")
    public ApiResponse<EstimationResult> analyze(@PathVariable String projectId, @RequestBody(required = false) EstimationRequest request) throws IOException {
        return ApiResponse.ok("工作量与成本估算完成", estimationService.analyzeProject(projectId, request));
    }

    @GetMapping("/latest")
    public ApiResponse<EstimationResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(estimationService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<EstimationReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("估算 Markdown 报告生成成功", estimationService.exportMarkdownReport(projectId));
    }
}
