package com.loan_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenAPIConfig {
	@Bean
    public OpenAPI loanServiceAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Loan Service API")
                .description("Complete loan management system with EMI, prepayment, and foreclosure")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Banking Team")
                    .email("support@bank.com")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
