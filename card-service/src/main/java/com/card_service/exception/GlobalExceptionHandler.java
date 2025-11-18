package com.card_service.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFound(CardNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Card Not Found")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidCardOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidCardOperationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Invalid Operation")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(CardLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleLimitExceeded(CardLimitExceededException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Limit Exceeded")
            .message(ex.getMessage())
            .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
    }
}
