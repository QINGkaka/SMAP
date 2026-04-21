package com.metriclab.model.dto;

public record ComplexitySummary(
        int fileCount,
        int methodCount,
        double averageComplexity,
        int maxComplexity,
        int highRiskMethodCount
) {
}
