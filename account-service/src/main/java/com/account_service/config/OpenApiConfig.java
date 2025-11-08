package com.account_service.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {
	 @Bean
	    public OpenAPI customOpenAPI() {
	        return new OpenAPI()
	                .info(new Info()
	                        .title("Account Service API")
	                        .version("1.0.0")
	                        .description("Banking Application - Account Management Microservice\n\n" +
	                                "Features:\n" +
	                                "- Complete CRUD operations for bank accounts\n" +
	                                "- Event Sourcing for account history\n" +
	                                "- Saga pattern for distributed transactions\n" +
	                                "- Outbox pattern for reliable event publishing\n" +
	                                "- Idempotency support for critical operations\n" +
	                                "- Circuit breaker and retry mechanisms\n" +
	                                "- CQRS for read/write separation")
	                        .contact(new Contact()
	                                .name("Banking Team")
	                                .email("support@banking.com"))
	                        .license(new License()
	                                .name("Apache 2.0")
	                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
	                .servers(List.of(
	                        new Server().url("http://localhost:8091").description("Local Development"),
	                        new Server().url("https://api.banking.com").description("Production")))
	                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
	                .components(new Components()
	                        .addSecuritySchemes("bearerAuth",
	                                new SecurityScheme()
	                                        .type(SecurityScheme.Type.HTTP)
	                                        .scheme("bearer")
	                                        .bearerFormat("JWT")
	                                        .description("JWT token from Keycloak")));
	    }
}
