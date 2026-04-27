package com.metriclab.model.dto;

public record ObjectOrientedSummary(
        int fileCount,
        int classCount,
        int interfaceCount,
        int methodCount,
        int fieldCount,
        double averageCbo,
        double averageRfc,
        int maxDit,
        int highRiskClassCount
) {
}
