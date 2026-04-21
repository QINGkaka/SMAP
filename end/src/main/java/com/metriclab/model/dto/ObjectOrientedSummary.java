package com.metriclab.model.dto;

public record ObjectOrientedSummary(
        int fileCount,
        int classCount,
        int interfaceCount,
        int methodCount,
        int fieldCount,
        double averageCbo,
        int maxDit,
        int highRiskClassCount
) {
}
