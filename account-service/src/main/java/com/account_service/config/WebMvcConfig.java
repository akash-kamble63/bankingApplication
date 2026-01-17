package com.account_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.account_service.interceptor.RateLimitInterceptor;
import com.account_service.patterns.IdempotencyInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/v1/accounts/**")
                .excludePathPatterns("/api/v1/accounts/health", "/api/v1/accounts/search");
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/beneficiaries/**")
                .excludePathPatterns("/api/v1/actuator/**");
    }

}