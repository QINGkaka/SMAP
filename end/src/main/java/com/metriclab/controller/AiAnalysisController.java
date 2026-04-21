package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.AiAnalysisResult;
import com.metriclab.service.AiAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/ai-analysis")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<AiAnalysisResult> analyze(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("智能质量分析完成", aiAnalysisService.analyzeProject(projectId));
    }

    @GetMapping("/latest")
    public ApiResponse<AiAnalysisResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(aiAnalysisService.latestResult(projectId));
    }
}
