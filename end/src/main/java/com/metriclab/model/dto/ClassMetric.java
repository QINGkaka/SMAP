package com.metriclab.model.dto;

public record ClassMetric(
        String className,
        String fileName,
        String sourceUploadName,
        String type,
        int cbo,
        int rfc,
        int dit,
        int noc,
        int noa,
        int noo,
        int cs,
        int wmc,
        int lcom,
        String riskLevel
) {
}
