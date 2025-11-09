package com.transaction_service.specification;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.transaction_service.DTO.TransactionFilterRequest;
import com.transaction_service.entity.Transaction;

import jakarta.persistence.criteria.Predicate;
public class TransactionSpecification {
	public static Specification<Transaction> filterTransactions(TransactionFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }
            
            if (filter.getAccountNumber() != null) {
                predicates.add(cb.equal(root.get("accountNumber"), filter.getAccountNumber()));
            }
            
            if (filter.getTypes() != null && !filter.getTypes().isEmpty()) {
                predicates.add(root.get("transactionType").in(filter.getTypes()));
            }
            
            if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(filter.getStatuses()));
            }
            
            if (filter.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filter.getMinAmount()));
            }
            
            if (filter.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filter.getMaxAmount()));
            }
            
            if (filter.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
            }
            
            if (filter.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
            }
            
            if (filter.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), filter.getCategory()));
            }
            
            if (filter.getPaymentMethod() != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), filter.getPaymentMethod()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
