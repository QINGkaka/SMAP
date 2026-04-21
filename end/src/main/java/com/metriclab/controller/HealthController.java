package com.metriclab.controller;

import com.metriclab.common.ApiResponse;
import com.metriclab.model.dto.HealthStatus;
import com.metriclab.storage.FileStorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final FileStorageService fileStorageService;

    public HealthController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/health")
    public ApiResponse<HealthStatus> health() {
        HealthStatus status = new HealthStatus(
                "metric-lab-backend",
                "UP",
                fileStorageService.rootPath().toString(),
                fileStorageService.initializedPaths(),
                OffsetDateTime.now()
        );
        return ApiResponse.ok(status);
    }
}
