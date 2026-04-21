package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record ProjectInfo(
        String id,
        String name,
        String language,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
