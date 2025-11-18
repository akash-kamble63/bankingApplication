package com.card_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.card_service.enums.CardTransactionStatus;
import com.card_service.enums.CardTransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardTransactionResponse {
	private Long id;
    private String transactionReference;
    private String cardToken;
    private CardTransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCity;
    private String merchantCountry;
    private CardTransactionStatus status;
    private Boolean isInternational;
    private Boolean isContactless;
    private BigDecimal rewardPointsEarned;
    private LocalDateTime createdAt;
}
