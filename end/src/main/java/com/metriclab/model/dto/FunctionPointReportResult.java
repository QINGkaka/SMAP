package com.metriclab.model.dto;

public record FunctionPointReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
