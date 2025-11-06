package com.account_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private String code;
    private String message;
    private T data;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Add these for better error handling
    private String correlationId;  // Track requests across services
    private List<String> errors;    // Multiple validation errors
    private Map<String, Object> metadata; // Pagination, etc.

    // Success with data
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    // Success without data
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code("SUCCESS")
                .message(message)
                .build();
    }

    // Error response
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
    
    // Error with multiple validation errors
    public static <T> ApiResponse<T> error(String code, String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .errors(errors)
                .build();
    }
}
