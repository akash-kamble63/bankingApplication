package com.transaction_service.DTOs;

import java.time.LocalDateTime;

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
public class ApiResponseDTO<T>{
	private String code;
    private String message;
    private T data;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return ApiResponseDTO.<T>builder()
            .code("200")
            .message(message)
            .data(data)
            .build();
    }
    
    public static <T> ApiResponseDTO<T> success(String message) {
        return ApiResponseDTO.<T>builder()
            .code("200")
            .message(message)
            .build();
    }
    
    public static <T> ApiResponseDTO<T> error(String code, String message) {
        return ApiResponseDTO.<T>builder()
            .code(code)
            .message(message)
            .build();
    }
}
