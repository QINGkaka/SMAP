package com.metriclab.model.dto;

public record UseCasePointReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
