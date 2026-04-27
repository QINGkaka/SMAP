package com.metriclab.model.dto;

public record FunctionPointDetailRequest(
        String name,
        Integer det,
        Integer ret,
        Integer ftr
) {
}
