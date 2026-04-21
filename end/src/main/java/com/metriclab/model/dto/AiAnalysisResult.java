package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AiAnalysisResult(
        String taskId,
        String projectId,
        String modelName,
        String overallAssessment,
        List<String> riskItems,
        List<String> refactoringSuggestions,
        List<String> testSuggestions,
        String markdown,
        OffsetDateTime analyzedAt
) {
}
