package com.metriclab.model.dto;

public record ObjectOrientedReportResult(
        String projectId,
        String taskId,
        String reportPath,
        String content
) {
}
