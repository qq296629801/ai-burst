package com.aiburst.mag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MagTemporalProperties.class)
public class MagConfiguration {
}
