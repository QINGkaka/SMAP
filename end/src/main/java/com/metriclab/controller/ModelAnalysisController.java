package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.AnalysisScopeRequest;
import com.metriclab.model.dto.ModelAnalysisReportResult;
import com.metriclab.model.dto.ModelAnalysisResult;
import com.metriclab.service.ModelAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/model-analysis")
public class ModelAnalysisController {

    private final ModelAnalysisService modelAnalysisService;

    public ModelAnalysisController(ModelAnalysisService modelAnalysisService) {
        this.modelAnalysisService = modelAnalysisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<ModelAnalysisResult> analyze(
            @PathVariable String projectId,
            @RequestBody(required = false) AnalysisScopeRequest request
    ) throws IOException {
        return ApiResponse.ok("模型文件度量完成", modelAnalysisService.analyzeProject(projectId, request));
    }

    @GetMapping("/latest")
    public ApiResponse<ModelAnalysisResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(modelAnalysisService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<ModelAnalysisReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("模型文件 Markdown 报告生成成功", modelAnalysisService.exportMarkdownReport(projectId));
    }
}
