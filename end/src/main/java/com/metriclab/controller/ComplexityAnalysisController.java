package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.ComplexityAnalysisResult;
import com.metriclab.model.dto.ComplexityReportResult;
import com.metriclab.service.ComplexityAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/complexity")
public class ComplexityAnalysisController {

    private final ComplexityAnalysisService complexityAnalysisService;

    public ComplexityAnalysisController(ComplexityAnalysisService complexityAnalysisService) {
        this.complexityAnalysisService = complexityAnalysisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<ComplexityAnalysisResult> analyze(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("圈复杂度分析完成", complexityAnalysisService.analyzeProject(projectId));
    }

    @GetMapping("/latest")
    public ApiResponse<ComplexityAnalysisResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(complexityAnalysisService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<ComplexityReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("圈复杂度 Markdown 报告生成成功", complexityAnalysisService.exportMarkdownReport(projectId));
    }
}
