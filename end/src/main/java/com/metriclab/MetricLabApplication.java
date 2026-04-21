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
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // .env is optional; startup should continue with regular environment variables.
        }
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
