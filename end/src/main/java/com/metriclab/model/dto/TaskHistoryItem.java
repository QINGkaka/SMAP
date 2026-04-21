package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record TaskHistoryItem(
        String taskId,
        String projectId,
        String type,
        String status,
        OffsetDateTime createdAt,
        String resultDirectory
) {
}
