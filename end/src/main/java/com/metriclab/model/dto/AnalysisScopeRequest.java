package com.metriclab.model.dto;

import java.util.List;

public record AnalysisScopeRequest(
        List<String> fileIds
) {
}
