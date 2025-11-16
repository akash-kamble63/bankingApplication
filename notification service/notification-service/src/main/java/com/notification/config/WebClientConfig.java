package com.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	@Value("${notification.sms.gateway-url:http://localhost:9000}")
    private String smsGatewayUrl;
    
    @Value("${notification.push.gateway-url:https://fcm.googleapis.com}")
    private String pushGatewayUrl;

    @Bean
    public WebClient smsWebClient() {
        return WebClient.builder()
            .baseUrl(smsGatewayUrl)
            .build();
    }

    @Bean
    public WebClient pushWebClient() {
        return WebClient.builder()
            .baseUrl(pushGatewayUrl)
            .build();
    }
}
