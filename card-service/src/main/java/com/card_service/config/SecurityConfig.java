package com.card_service.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {
	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/actuator/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api/v1/cards/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/v1/cards/admin/**").hasRole("ADMIN")
                
                // Card endpoints - require authentication
                .requestMatchers("/api/v1/cards/**").hasAnyRole("USER", "ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    /**
     * Convert JWT claims to Spring Security authorities
     * Extracts roles from Keycloak realm_access and resource_access
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    /**
     * Custom converter to extract Keycloak roles
     */
    public static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            Collection<GrantedAuthority> authorities = extractRoles(realmAccess);
            
            // Extract client roles (resource_access)
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                resourceAccess.values().forEach(resource -> {
                    if (resource instanceof Map) {
                        authorities.addAll(extractRoles((Map<String, Object>) resource));
                    }
                });
            }
            
            log.debug("Extracted authorities: {}", authorities);
            return authorities;
        }
        
        private Collection<GrantedAuthority> extractRoles(Map<String, Object> access) {
            if (access == null || !access.containsKey("roles")) {
                return List.of();
            }
            
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) access.get("roles");
            
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        }
    }
}
