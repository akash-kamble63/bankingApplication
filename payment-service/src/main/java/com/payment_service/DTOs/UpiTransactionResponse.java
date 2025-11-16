package com.payment_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpiTransactionResponse {
    private boolean success;
    private String transactionId;
    private String errorMessage;
}
