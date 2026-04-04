package com.aiburst.mag.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc 分组 {@code mag}，与《多Agent协作技术方案》§19.8 一致；全局路径见 {@code application.yml}。
 */
@Configuration
public class MagOpenApiConfiguration {

    @Bean
    public GroupedOpenApi magOpenApi() {
        return GroupedOpenApi.builder()
                .group("mag")
                .displayName("MAG — 多 Agent")
                .packagesToScan("com.aiburst.mag")
                .build();
    }
}
