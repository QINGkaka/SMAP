package com.metriclab.model.dto;

import java.util.List;

public record FunctionPointRequest(
        FunctionPointCount externalInputs,
        FunctionPointCount externalOutputs,
        FunctionPointCount externalInquiries,
        FunctionPointCount internalLogicalFiles,
        FunctionPointCount externalInterfaceFiles,
        String countMode,
        List<FunctionPointDetailRequest> externalInputDetails,
        List<FunctionPointDetailRequest> externalOutputDetails,
        List<FunctionPointDetailRequest> externalInquiryDetails,
        List<FunctionPointDetailRequest> internalLogicalFileDetails,
        List<FunctionPointDetailRequest> externalInterfaceFileDetails,
        Integer generalSystemCharacteristicTotal,
        List<Integer> generalSystemCharacteristics
) {
}
