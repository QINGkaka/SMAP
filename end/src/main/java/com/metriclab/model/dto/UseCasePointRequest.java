package com.metriclab.model.dto;

import java.util.List;

public record UseCasePointRequest(
        int simpleActors,
        int averageActors,
        int complexActors,
        int simpleUseCases,
        int averageUseCases,
        int complexUseCases,
        Double technicalFactorTotal,
        Double environmentalFactorTotal,
        Double productivityHoursPerUseCasePoint,
        List<Integer> technicalFactors,
        List<Integer> environmentalFactors
) {
}
