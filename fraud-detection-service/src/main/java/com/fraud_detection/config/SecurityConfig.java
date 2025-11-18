package com.fraud_detection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	 @Bean
	    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	        http
	                .csrf(AbstractHttpConfigurer::disable)
	                .authorizeHttpRequests(auth -> auth
	                        // Actuator endpoints for health checks
	                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
	                        .requestMatchers("/actuator/**").hasRole("ADMIN")
	                        
	                        // Swagger/OpenAPI endpoints
	                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
	                        
	                        // API endpoints
	                        .requestMatchers(HttpMethod.POST, "/api/v1/fraud/check").hasAnyRole("SYSTEM", "FRAUD_ANALYST")
	                        .requestMatchers(HttpMethod.GET, "/api/v1/fraud/**").hasAnyRole("FRAUD_ANALYST", "ADMIN", "USER")
	                        .requestMatchers(HttpMethod.POST, "/api/v1/fraud/*/review").hasRole("FRAUD_ANALYST")
	                        
	                        // All other requests must be authenticated
	                        .anyRequest().authenticated()
	                )
	                .oauth2ResourceServer(oauth2 -> oauth2
	                        .jwt(jwt -> jwt
	                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
	                        )
	                )
	                .sessionManagement(session -> session
	                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	                );
	        
	        return http.build();
	    }
	    
	    @Bean
	    public JwtAuthenticationConverter jwtAuthenticationConverter() {
	        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
	        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
	        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
	        
	        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
	        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
	        
	        return jwtAuthenticationConverter;
	    }
}
