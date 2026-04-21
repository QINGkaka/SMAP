package com.metriclab.model.dto;

public record EstimationRequest(
        String mode,
        Double kloc,
        Double costPerPersonMonth
) {
}
