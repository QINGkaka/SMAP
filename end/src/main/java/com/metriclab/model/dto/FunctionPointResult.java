package com.metriclab.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FunctionPointResult(
        String taskId,
        String projectId,
        int externalInputs,
        int externalOutputs,
        int externalInquiries,
        int internalLogicalFiles,
        int externalInterfaceFiles,
        int unadjustedFunctionPoints,
        int generalSystemCharacteristicTotal,
        double valueAdjustmentFactor,
        double adjustedFunctionPoints,
        String countMode,
        List<FunctionPointComponentSummary> componentSummaries,
        List<FunctionPointDetailResult> detailItems,
        OffsetDateTime analyzedAt
) {
}
