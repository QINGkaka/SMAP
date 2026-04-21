package com.metriclab.service;

import com.metriclab.storage.FileStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ThresholdConfigService {

    private final FileStorageService fileStorageService;

    public ThresholdConfigService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public Map<String, Map<String, Double>> getThresholds() throws IOException {
        Map<?, ?> raw = fileStorageService.readJson(fileStorageService.thresholdsPath(), Map.class);
        Map<String, Map<String, Double>> thresholds = new LinkedHashMap<>();
        raw.forEach((metric, values) -> {
            if (metric == null || !(values instanceof Map<?, ?> valueMap)) {
                return;
            }
            Map<String, Double> normalized = new LinkedHashMap<>();
            valueMap.forEach((level, value) -> {
                if (level != null && value instanceof Number number) {
                    normalized.put(level.toString(), number.doubleValue());
                }
            });
            thresholds.put(metric.toString(), normalized);
        });
        return thresholds;
    }

    public synchronized Map<String, Map<String, Double>> saveThresholds(Map<String, Map<String, Double>> request) throws IOException {
        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("阈值配置不能为空");
        }
        Map<String, Map<String, Double>> normalized = new LinkedHashMap<>();
        request.forEach((metric, values) -> {
            if (metric == null || metric.isBlank() || values == null) {
                return;
            }
            double low = valueOf(values, "low", 0);
            double medium = valueOf(values, "medium", low);
            double high = valueOf(values, "high", medium);
            if (medium < low || high < medium) {
                throw new IllegalArgumentException(metric + " 的阈值必须满足 low <= medium <= high");
            }
            normalized.put(metric, Map.of("low", low, "medium", medium, "high", high));
        });
        fileStorageService.writeJson(fileStorageService.thresholdsPath(), normalized);
        return getThresholds();
    }

    private double valueOf(Map<String, Double> values, String key, double defaultValue) {
        Double value = values.get(key);
        if (value == null || value < 0) {
            return defaultValue;
        }
        return value;
    }
}
