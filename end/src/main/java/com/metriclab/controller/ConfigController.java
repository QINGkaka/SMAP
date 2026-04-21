package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.service.ThresholdConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ThresholdConfigService thresholdConfigService;

    public ConfigController(ThresholdConfigService thresholdConfigService) {
        this.thresholdConfigService = thresholdConfigService;
    }

    @GetMapping("/thresholds")
    public ApiResponse<Map<String, Map<String, Double>>> getThresholds() throws IOException {
        return ApiResponse.ok(thresholdConfigService.getThresholds());
    }

    @PutMapping("/thresholds")
    public ApiResponse<Map<String, Map<String, Double>>> saveThresholds(@RequestBody Map<String, Map<String, Double>> request) throws IOException {
        return ApiResponse.ok("阈值配置已保存", thresholdConfigService.saveThresholds(request));
    }
}
