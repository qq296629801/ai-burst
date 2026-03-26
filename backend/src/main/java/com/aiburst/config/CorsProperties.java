package com.aiburst.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aiburst.cors")
public class CorsProperties {
    private String allowedOrigins = "http://localhost:5173";
}
