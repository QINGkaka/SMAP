package com.metriclab.model.dto;

public record LocFileMetric(
        String fileName,
        String sourceUploadName,
        int totalLines,
        int sourceLines,
        int commentLines,
        int blankLines,
        double commentRate
) {
}
