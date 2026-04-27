package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record UseCasePointResult(
        String taskId,
        String projectId,
        int actorWeight,
        int useCaseWeight,
        int unadjustedUseCasePoints,
        double technicalFactorTotal,
        double technicalComplexityFactor,
        double environmentalFactorTotal,
        double environmentalComplexityFactor,
        double useCasePoints,
        double productivityHoursPerUseCasePoint,
        double estimatedHours,
        double estimatedPersonMonths,
        OffsetDateTime analyzedAt
) {
}
