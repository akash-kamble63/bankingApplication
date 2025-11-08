package com.account_service.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferSagaData {
    private String sagaId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String transactionReference;
    private Long userId;
    
    // Step results
    private boolean accountValidated;
    private boolean fundsReserved;
    private boolean fundsDebited;
    private boolean fundsCredited;
    private boolean notificationSent;
    
    // Compensation data
    private String holdReference;
    private String debitTransactionId;
    private String creditTransactionId;
}
