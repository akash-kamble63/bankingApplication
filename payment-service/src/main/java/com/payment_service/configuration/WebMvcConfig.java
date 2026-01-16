package com.payment_service.configuration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.payment_service.intercepter.IdempotencyInterceptor;

@Configuration
@EnableScheduling
public class WebMvcConfig {
    @Bean
    public FilterRegistrationBean<IdempotencyInterceptor> idempotencyFilter(
            IdempotencyInterceptor idempotencyInterceptor) {

        FilterRegistrationBean<IdempotencyInterceptor> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(idempotencyInterceptor);
        registrationBean.addUrlPatterns("/api/v1/payments/*");
        registrationBean.setOrder(1); // Execute early in filter chain
        registrationBean.setName("idempotencyFilter");

        return registrationBean;
    }
}
