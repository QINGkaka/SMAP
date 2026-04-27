package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ObjectOrientedAnalysisResult(
        String taskId,
        String projectId,
        ObjectOrientedSummary summary,
        List<ClassMetric> classes,
        OffsetDateTime analyzedAt,
        List<String> analyzedFileIds
) {
}
