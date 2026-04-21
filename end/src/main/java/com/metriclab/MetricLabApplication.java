package com.metriclab;

import com.metriclab.config.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileStorageProperties.class)
public class MetricLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricLabApplication.class, args);
    }
}
