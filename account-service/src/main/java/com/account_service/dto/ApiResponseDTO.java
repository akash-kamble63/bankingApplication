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
public class ApiResponseDTO<T> {
	private boolean success;
	private String code;
	private String message;
	private T data;
	private Map<String, Object> error;
	private LocalDateTime timestamp;

	public static <T> ApiResponseDTO<T> success(T data, String message) {
		return ApiResponseDTO.<T>builder()
				.success(true)
				.code("200")
				.message(message)
				.data(data)
				.timestamp(LocalDateTime.now())
				.build();
	}

	public static <T> ApiResponseDTO<T> success(String message) {
		return ApiResponseDTO.<T>builder()
				.success(true)
				.code("200")
				.message(message)
				.timestamp(LocalDateTime.now())
				.build();
	}

	public static <T> ApiResponseDTO<T> error(String code, String message) {
		return ApiResponseDTO.<T>builder()
				.success(false)
				.code(code)
				.message(message)
				.timestamp(LocalDateTime.now())
				.build();
	}
}
