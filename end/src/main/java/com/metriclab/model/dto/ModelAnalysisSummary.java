package com.metriclab.model.dto;

public record ModelAnalysisSummary(
        int fileCount,
        int classCount,
        int interfaceCount,
        int attributeCount,
        int operationCount,
        int inheritanceRelationCount,
        int highRiskClassCount
) {
}
