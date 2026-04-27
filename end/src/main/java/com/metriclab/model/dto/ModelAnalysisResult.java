package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ModelAnalysisResult(
        String taskId,
        String projectId,
        ModelAnalysisSummary summary,
        List<ModelClassMetric> classes,
        OffsetDateTime analyzedAt,
        List<String> analyzedFileIds
) {
}
