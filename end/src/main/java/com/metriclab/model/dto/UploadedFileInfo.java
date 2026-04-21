package com.metriclab.model.dto;

import java.time.OffsetDateTime;

public record UploadedFileInfo(
        String id,
        String originalName,
        String storedName,
        String fileType,
        long size,
        OffsetDateTime uploadedAt
) {
}
