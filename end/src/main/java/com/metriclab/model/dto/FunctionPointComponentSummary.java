package com.metriclab.model.dto;

public record FunctionPointComponentSummary(
        String code,
        String label,
        int itemCount,
        int lowCount,
        int averageCount,
        int highCount,
        int functionPoints
) {
}
