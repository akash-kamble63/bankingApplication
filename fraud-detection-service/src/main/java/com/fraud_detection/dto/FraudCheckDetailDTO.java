package com.fraud_detection.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fraud_detection.enums.FraudStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckDetailDTO {
	 private Long id;
	    private String transactionId;
	    private String accountId;
	    private String userId;
	    private BigDecimal amount;
	    private String currency;
	    private FraudStatus status;
	    private Double riskScore;
	    private List<String> fraudReasons;
	    private String merchantName;
	    private String merchantCategory;
	    private String transactionType;
	    private String locationCountry;
	    private String locationCity;
	    private Boolean reviewed;
	    private String reviewedBy;
	    private LocalDateTime reviewedAt;
	    private String reviewNotes;
	    private LocalDateTime createdAt;
}
