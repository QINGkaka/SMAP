package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.XmlExportResult;
import com.metriclab.service.XmlExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/projects/{projectId}/export")
public class XmlExportController {

    private final XmlExportService xmlExportService;

    public XmlExportController(XmlExportService xmlExportService) {
        this.xmlExportService = xmlExportService;
    }

    @GetMapping("/xml")
    public ApiResponse<XmlExportResult> exportXml(@PathVariable String projectId) throws IOException {
        return ApiResponse.ok("XML 度量结果导出成功", xmlExportService.exportProjectMetrics(projectId));
    }
}
