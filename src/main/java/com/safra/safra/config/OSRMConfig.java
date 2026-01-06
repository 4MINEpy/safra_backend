package com.safra.safra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "osrm")
@Data
public class OSRMConfig {
    private String baseUrl = "http://router.project-osrm.org";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}