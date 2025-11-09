package com.transaction_service.config;

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
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.util.*;
import java.util.stream.Collectors;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
	 @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	    private String issuerUri;
	    
	    @Bean
	    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	        http
	            .csrf(csrf -> csrf.disable())
	            .sessionManagement(session -> 
	                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
	            .authorizeHttpRequests(authz -> authz
	                .requestMatchers("/actuator/**", "/v3/api-docs/**", 
	                               "/swagger-ui/**", "/swagger-ui.html").permitAll()
	                .requestMatchers("/api/v1/transactions/admin/**").hasRole("ADMIN")
	                .requestMatchers("/api/v1/transactions/**").hasAnyRole("USER", "ADMIN", "ACCOUNTANT")
	                .anyRequest().authenticated()
	            )
	            .oauth2ResourceServer(oauth2 -> oauth2
	                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
	            );
	        
	        return http.build();
	    }
	    
	    @Bean
	    public JwtDecoder jwtDecoder() {
	        return JwtDecoders.fromIssuerLocation(issuerUri);
	    }
	    
	    @Bean
	    public JwtAuthenticationConverter jwtAuthenticationConverter() {
	        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
	        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
	        return converter;
	    }
	    
	    public static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
	        @Override
	        public Collection<GrantedAuthority> convert(Jwt jwt) {
	            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
	            Collection<GrantedAuthority> authorities = extractRoles(realmAccess);
	            
	            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
	            if (resourceAccess != null) {
	                resourceAccess.values().forEach(resource -> {
	                    if (resource instanceof Map) {
	                        authorities.addAll(extractRoles((Map<String, Object>) resource));
	                    }
	                });
	            }
	            
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
