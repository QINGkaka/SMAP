package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.FunctionPointReportResult;
import com.metriclab.model.dto.FunctionPointRequest;
import com.metriclab.model.dto.FunctionPointResult;
import com.metriclab.service.FunctionPointService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/function-point")
public class FunctionPointController {

    private final FunctionPointService functionPointService;

    public FunctionPointController(FunctionPointService functionPointService) {
        this.functionPointService = functionPointService;
    }

    @PostMapping("/analyze")
    public ApiResponse<FunctionPointResult> analyze(@PathVariable String projectId, @RequestBody(required = false) FunctionPointRequest request) throws IOException {
        return ApiResponse.ok("功能点度量完成", functionPointService.analyzeProject(projectId, request));
    }

    @GetMapping("/latest")
    public ApiResponse<FunctionPointResult> latest(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok(functionPointService.latestResult(projectId));
    }

    @GetMapping("/report")
    public ApiResponse<FunctionPointReportResult> exportReport(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("功能点 Markdown 报告生成成功", functionPointService.exportMarkdownReport(projectId));
    }
}
