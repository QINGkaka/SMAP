package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record EstimationResult(
        String taskId,
        String projectId,
        String mode,
        String modeLabel,
        double kloc,
        String scaleSource,
        double effortPersonMonths,
        double developmentMonths,
        double averageStaff,
        double costPerPersonMonth,
        double estimatedCost,
        OffsetDateTime analyzedAt
) {
}
