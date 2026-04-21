package com.metriclab;

import com.metriclab.config.FileStorageProperties;
import com.metriclab.config.LargeModelProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({FileStorageProperties.class, LargeModelProperties.class})
public class MetricLabApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(MetricLabApplication.class, args);
    }

    private static void loadDotEnv() {
        List<Path> candidates = List.of(Path.of(".env"), Path.of("../.env"));
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                loadDotEnvFile(candidate);
            }
        }
    }

    private static void loadDotEnvFile(Path path) {
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, equalsIndex).trim();
                String value = unquote(trimmed.substring(equalsIndex + 1).trim());
                System.setProperty(key, value);
                String springPropertyKey = toSpringPropertyKey(key);
                if (springPropertyKey != null) {
                    System.setProperty(springPropertyKey, value);
                }
            }
        } catch (IOException ignored) {
            // .env is optional; startup should continue with regular environment variables.
        }
    }

    private static String toSpringPropertyKey(String key) {
        return switch (key) {
            case "LARGE_MODEL_ENABLED" -> "metric.large-model.enabled";
            case "LARGE_MODEL_PROVIDER" -> "metric.large-model.provider";
            case "LARGE_MODEL_BASE_URL" -> "metric.large-model.base-url";
            case "LARGE_MODEL_API_KEY", "ARK_API_KEY" -> "metric.large-model.api-key";
            case "LARGE_MODEL_MODEL", "ARK_MODEL" -> "metric.large-model.model";
            case "LARGE_MODEL_REASONING_EFFORT", "ARK_REASONING_EFFORT" -> "metric.large-model.reasoning-effort";
            case "LARGE_MODEL_MAX_COMPLETION_TOKENS", "ARK_MAX_COMPLETION_TOKENS" -> "metric.large-model.max-completion-tokens";
            case "LARGE_MODEL_TIMEOUT_SECONDS" -> "metric.large-model.timeout-seconds";
            default -> null;
        };
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
