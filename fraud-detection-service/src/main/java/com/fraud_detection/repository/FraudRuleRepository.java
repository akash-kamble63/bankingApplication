package com.fraud_detection.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.fraud_detection.entity.FraudRule;

@Repository
public interface FraudRuleRepository {
	List<FraudRule> findByEnabledTrueOrderByPriorityAsc();

	Optional<FraudRule> findByRuleName(String ruleName);

	List<FraudRule> findByRuleType(String ruleType);
}
