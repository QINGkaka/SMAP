package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record HealthStatus(
        String application,
        String status,
        String storageRoot,
        List<String> initializedPaths,
        OffsetDateTime checkedAt
) {
}
