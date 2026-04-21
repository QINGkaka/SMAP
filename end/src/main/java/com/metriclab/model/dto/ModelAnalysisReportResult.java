package com.metriclab.model.dto;

public record ModelAnalysisReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
