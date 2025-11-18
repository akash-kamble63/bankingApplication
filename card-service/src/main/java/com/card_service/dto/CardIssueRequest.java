package com.card_service.dto;

import java.math.BigDecimal;

import com.card_service.enums.CardNetwork;
import com.card_service.enums.CardType;
import com.card_service.enums.CardVariant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardIssueRequest {
	@NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
    
    @NotNull(message = "Card type is required")
    private CardType cardType;
    
    @NotNull(message = "Card network is required")
    private CardNetwork cardNetwork;
    
    private CardVariant cardVariant;
    
    private Boolean isVirtual = false;
    
    private BigDecimal dailyLimit;
    
    private BigDecimal monthlyLimit;
    
    private BigDecimal perTransactionLimit;
    
    private BigDecimal creditLimit; // For credit cards
    
    private String deliveryAddress;
}
