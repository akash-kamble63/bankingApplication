package com.transaction_service.DTOs;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSagaData {
	private String sagaId;
    private String transactionReference;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private Long userId;
    
    // Step tracking
    private boolean accountValidated;
    private boolean fraudChecked;
    private boolean fundsReserved;
    private boolean fundsDebited;
    private boolean fundsCredited;
    private boolean notificationSent;
    
    // Compensation data
    private String holdReference;
    private String debitTransactionId;
    private String creditTransactionId;
}
