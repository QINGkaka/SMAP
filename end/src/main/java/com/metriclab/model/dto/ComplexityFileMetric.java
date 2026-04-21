package com.metriclab.model.dto;

public record ComplexityFileMetric(
        String fileName,
        String sourceUploadName,
        int declaredMethodCount,
        int executableMethodCount,
        String status
) {
}
