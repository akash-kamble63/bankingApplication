package com.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // Public endpoints - Account Management
                    "/api/users/register",
                    "/api/users/test",
                    "/api/users/resend-verification",

                    
                    // Actuator
                    "/actuator/health",

                    // Swagger / OpenAPI endpoints (Springdoc 2.x)
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs/swagger-config"
                ).permitAll()
                
                // Admin only endpoints
                .requestMatchers(
                    "/api/users/{userId}/activate",
                    "/api/users/{userId}/deactivate",
                    "/api/users"
                ).hasRole("ADMIN")
                
                // Authenticated endpoints (requires valid JWT)
                .requestMatchers(
                    "/api/users/me",
                    "/api/users/me/**",
                    "/api/users/password/change"  // Change password requires auth
                ).authenticated()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
