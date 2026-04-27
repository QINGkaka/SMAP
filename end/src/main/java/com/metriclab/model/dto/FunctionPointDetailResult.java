package com.metriclab.model.dto;

public record FunctionPointDetailResult(
        String code,
        String label,
        String name,
        int det,
        Integer ret,
        Integer ftr,
        String complexity,
        int functionPoints
) {
}
