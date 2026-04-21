package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ComplexityAnalysisResult(
        String taskId,
        String projectId,
        ComplexitySummary summary,
        List<ComplexityFileMetric> files,
        List<MethodComplexityMetric> methods,
        OffsetDateTime analyzedAt
) {
}
