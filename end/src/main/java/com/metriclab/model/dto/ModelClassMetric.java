package com.metriclab.model.dto;

public record ModelClassMetric(
        String className,
        String type,
        String sourceUploadName,
        int attributeCount,
        int operationCount,
        int childCount,
        int inheritanceDepth,
        int classSize,
        String parentName,
        String riskLevel
) {
}
