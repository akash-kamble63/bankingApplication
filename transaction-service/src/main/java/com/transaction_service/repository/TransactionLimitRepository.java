package com.transaction_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transaction_service.entity.TransactionLimit;
import com.transaction_service.enums.LimitType;
import com.transaction_service.enums.TransactionType;

public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {
	Optional<TransactionLimit> findByUserIdAndTransactionTypeAndLimitType(
            Long userId, TransactionType type, LimitType limitType);
}
