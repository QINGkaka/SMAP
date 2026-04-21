package com.metriclab.model.dto;

public record ComplexityReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
