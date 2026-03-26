package com.aiburst.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aiburst.jwt")
public class JwtProperties {
    private String secret = "change-me";
    private int expireMinutes = 120;
}
