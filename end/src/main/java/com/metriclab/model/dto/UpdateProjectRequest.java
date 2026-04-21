package com.metriclab.model.dto;

public record UpdateProjectRequest(
        String name,
        String language,
        String description
) {
}
