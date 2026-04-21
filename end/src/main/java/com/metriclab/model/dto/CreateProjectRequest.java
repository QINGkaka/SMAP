package com.metriclab.model.dto;

public record CreateProjectRequest(
        String name,
        String language,
        String description
) {
}
