package com.metriclab.model.dto;

public record EstimationReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
