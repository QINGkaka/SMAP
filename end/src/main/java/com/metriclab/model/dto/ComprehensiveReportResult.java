package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record ComprehensiveReportResult(
        String projectId,
        String reportPath,
        String content,
        OffsetDateTime exportedAt
) {
}
