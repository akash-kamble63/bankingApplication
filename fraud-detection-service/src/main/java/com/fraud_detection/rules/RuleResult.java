package com.fraud_detection.rules;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleResult {
	private boolean passed;
    private double riskScore;
    private List<String> reasons;
}
