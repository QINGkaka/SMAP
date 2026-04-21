package com.metriclab.model.dto;

public record MethodComplexityMetric(
        String methodName,
        String fileName,
        String sourceUploadName,
        int startLine,
        int endLine,
        int cyclomaticComplexity,
        String riskLevel
) {
}
