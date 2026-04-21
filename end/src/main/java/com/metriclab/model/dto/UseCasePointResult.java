package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record UseCasePointResult(
        String taskId,
        String projectId,
        int actorWeight,
        int useCaseWeight,
        int unadjustedUseCasePoints,
        int technicalFactorTotal,
        double technicalComplexityFactor,
        int environmentalFactorTotal,
        double environmentalComplexityFactor,
        double useCasePoints,
        double productivityHoursPerUseCasePoint,
        double estimatedHours,
        double estimatedPersonMonths,
        OffsetDateTime analyzedAt
) {
}
