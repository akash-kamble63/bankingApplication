package com.payment_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaResult {
	private String sagaId;
    private boolean success;
    private String errorMessage;
    
    public static SagaResult success(String sagaId, String message) {
        return SagaResult.builder()
            .sagaId(sagaId)
            .success(true)
            .errorMessage(message)
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
