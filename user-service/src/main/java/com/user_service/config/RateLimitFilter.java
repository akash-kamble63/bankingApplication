package com.user_service.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user_service.dto.ApiResponse;
import com.user_service.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

	private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final JwtDecoder jwtDecoder;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String endpoint = getEndpointKey(request);
        String ipAddress = getClientIp(request);
        
        // Check rate limit by IP
        if (!rateLimitService.isAllowedForIp(ipAddress, endpoint)) {
            handleRateLimitExceeded(response, "Too many requests from your IP address. Please try again later.");
            return;
        }
        
        // If user is authenticated, check user-specific rate limit
        String userId = extractUserIdFromRequest(request);
        if (userId != null && !rateLimitService.isAllowedForUser(userId, endpoint)) {
            handleRateLimitExceeded(response, "Too many requests. Please try again later.");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract endpoint key from request
     */
    private String getEndpointKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Map specific endpoints
        if (path.contains("/login")) return "login";
        if (path.contains("/register")) return "register";
        if (path.contains("/password/forgot")) return "forgot-password";
        if (path.contains("/password/reset")) return "reset-password";
        
        return "default";
    }
    
    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Extract user ID from JWT token
     */
    private String extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
            	String token = authHeader.substring(7);
            	Jwt jwt = jwtDecoder.decode(token);
            	return jwt.getClaimAsString("sub");
            } catch (Exception e) {
                log.debug("Failed to extract user ID from token");
            }
        }
        return null;
    }
    
    /**
     * Handle rate limit exceeded
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        
        ApiResponse<Void> apiResponse = ApiResponse.error("429", message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for health check and swagger
        return path.contains("/actuator/health") ||
               path.contains("/swagger-ui") ||
               path.contains("/v3/api-docs");
    }

}
