package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.AnalysisScopeRequest;
import com.metriclab.model.dto.ObjectOrientedAnalysisResult;
import com.metriclab.model.dto.ObjectOrientedReportResult;
import com.metriclab.service.ObjectOrientedAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/oo")
public class ObjectOrientedAnalysisController {

    private final ObjectOrientedAnalysisService objectOrientedAnalysisService;

    public ObjectOrientedAnalysisController(ObjectOrientedAnalysisService objectOrientedAnalysisService) {
        this.objectOrientedAnalysisService = objectOrientedAnalysisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<ObjectOrientedAnalysisResult> analyze(
            @PathVariable String projectId,
            @RequestBody(required = false) AnalysisScopeRequest request
    ) throws IOException {
        return ApiResponse.ok("面向对象 CK/LK 度量完成", objectOrientedAnalysisService.analyzeProject(projectId, request));
    }

    @GetMapping("/latest")
    public ApiResponse<ObjectOrientedAnalysisResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(objectOrientedAnalysisService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<ObjectOrientedReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("面向对象 Markdown 报告生成成功", objectOrientedAnalysisService.exportMarkdownReport(projectId));
    }
}
