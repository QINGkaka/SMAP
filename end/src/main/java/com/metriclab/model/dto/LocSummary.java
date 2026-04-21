package com.metriclab.model.dto;

public record LocSummary(
        int fileCount,
        int totalLines,
        int sourceLines,
        int commentLines,
        int blankLines,
        double commentRate
) {
}
