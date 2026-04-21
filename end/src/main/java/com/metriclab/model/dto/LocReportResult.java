package com.metriclab.model.dto;

public record LocReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
