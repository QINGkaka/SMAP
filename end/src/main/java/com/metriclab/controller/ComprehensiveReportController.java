package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.ComprehensiveReportResult;
import com.metriclab.service.ComprehensiveReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/report")
public class ComprehensiveReportController {

    private final ComprehensiveReportService comprehensiveReportService;

    public ComprehensiveReportController(ComprehensiveReportService comprehensiveReportService) {
        this.comprehensiveReportService = comprehensiveReportService;
    }

    @GetMapping("/comprehensive")
    public ApiResponse<ComprehensiveReportResult> export(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("综合 Markdown 报告生成成功", comprehensiveReportService.export(projectId));
    }
}
