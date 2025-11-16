package com.payment_service.configuration;

import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

@Configuration
public class WebClientConfig {
	@Value("${services.account-service.url:http://localhost:8081}")
	private String accountServiceUrl;

	@Value("${services.fraud-service.url:http://localhost:8084}")
	private String fraudServiceUrl;

	@Value("${services.merchant-service.url:http://localhost:8085}")
	private String merchantServiceUrl;

	@Value("${services.notification-service.url:http://localhost:8086}")
	private String notificationServiceUrl;

	@Bean
	public WebClient accountServiceWebClient() {
		return buildWebClient(accountServiceUrl);
	}

	@Bean
	public WebClient fraudServiceWebClient() {
		return buildWebClient(fraudServiceUrl);
	}

	@Bean
	public WebClient merchantServiceWebClient() {
		return buildWebClient(merchantServiceUrl);
	}

	@Bean
	public WebClient notificationServiceWebClient() {
		return buildWebClient(notificationServiceUrl);
	}

	@Bean
	public WebClient gatewayWebClient() {
		return buildWebClient("https://api.razorpay.com");
	}

	private WebClient buildWebClient(String baseUrl) {
		HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
				.responseTimeout(Duration.ofSeconds(10))
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
						.addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

		return WebClient.builder().baseUrl(baseUrl).clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}
}
