package com.metriclab.model.dto;

import java.util.List;

public record FunctionPointRequest(
        FunctionPointCount externalInputs,
        FunctionPointCount externalOutputs,
        FunctionPointCount externalInquiries,
        FunctionPointCount internalLogicalFiles,
        FunctionPointCount externalInterfaceFiles,
        Integer generalSystemCharacteristicTotal,
        List<Integer> generalSystemCharacteristics
) {
}
