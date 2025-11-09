package com.transaction_service.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.transaction_service.enums.TransactionStatus;
import com.transaction_service.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionFilterRequest {
	private Long userId;
    private String accountNumber;
    private List<TransactionType> types;
    private List<TransactionStatus> statuses;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String category;
    private String paymentMethod;
    
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
}
