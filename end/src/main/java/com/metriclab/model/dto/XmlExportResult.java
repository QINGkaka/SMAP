package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record XmlExportResult(
        String projectId,
        String reportPath,
        String content,
        OffsetDateTime exportedAt
) {
}
