package com.card_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.card_service.enums.CardNetwork;
import com.card_service.enums.CardStatus;
import com.card_service.enums.CardType;
import com.card_service.enums.CardVariant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
	private Long id;
    private String cardReference;
    private Long userId;
    private Long accountId;
    private String cardToken;
    private String maskedCardNumber; 
    private String cardHolderName;
    private CardType cardType;
    private CardNetwork cardNetwork;
    private CardVariant cardVariant;
    private Boolean isVirtual;
    private String expiryDate; 
    private CardStatus status;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailySpent;
    private BigDecimal monthlySpent;
    private BigDecimal rewardPoints;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
    private Boolean contactlessEnabled;
    private Boolean onlineTransactionsEnabled;
    private Boolean internationalTransactionsEnabled;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
