package com.loan_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	@Bean
    public WebClient accountServiceWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8081")
            .build();
    }

    @Bean
    public WebClient creditBureauWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:9001")
            .build();
    }

    @Bean
    public WebClient fraudServiceWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8084")
            .build();
    }
}
