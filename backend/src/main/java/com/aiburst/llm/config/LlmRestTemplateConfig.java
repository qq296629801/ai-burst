package com.aiburst.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LlmRestTemplateConfig {

    @Bean
    public RestTemplate llmRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(20_000);
        f.setReadTimeout(120_000);
        return new RestTemplate(f);
    }
}
