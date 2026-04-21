package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.LocAnalysisResult;
import com.metriclab.model.dto.LocReportResult;
import com.metriclab.service.LocAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/loc")
public class LocAnalysisController {

    private final LocAnalysisService locAnalysisService;

    public LocAnalysisController(LocAnalysisService locAnalysisService) {
        this.locAnalysisService = locAnalysisService;
    }

    @PostMapping("/analyze")
    public ApiResponse<LocAnalysisResult> analyze(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("代码行度量完成", locAnalysisService.analyzeProject(projectId));
    }

    @GetMapping("/latest")
    public ApiResponse<LocAnalysisResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(locAnalysisService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<LocReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("LoC Markdown 报告生成成功", locAnalysisService.exportMarkdownReport(projectId));
    }
}
