package com.transaction_service.client;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResult {
	private String transactionReference;
    private BigDecimal finalScore;
    private String recommendation;
    private List<String> flags;
}
