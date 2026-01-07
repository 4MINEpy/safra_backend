package com.safra.safra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded files from /uploads/** URL path
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadAbsolutePath = uploadPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadAbsolutePath)
                .setCachePeriod(3600); // Cache for 1 hour
    }
}
