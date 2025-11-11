package com.transaction_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SagaResult {
	private String sagaId;
    private boolean success;
    private String message;
    private String errorMessage;
    
    public static SagaResult success(String sagaId, String message) {
        return SagaResult.builder()
            .sagaId(sagaId)
            .success(true)
            .message(message)
            .build();
    }
    
    public static SagaResult failure(String sagaId, String errorMessage) {
        return SagaResult.builder()
            .sagaId(sagaId)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
