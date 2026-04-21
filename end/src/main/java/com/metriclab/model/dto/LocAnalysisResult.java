package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LocAnalysisResult(
        String taskId,
        String projectId,
        LocSummary summary,
        List<LocFileMetric> files,
        OffsetDateTime analyzedAt
) {
}
