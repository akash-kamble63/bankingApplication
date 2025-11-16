package com.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment_service.entity.PaymentEvent;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long>{
	List<PaymentEvent> findByAggregateIdOrderBySequenceAsc(String aggregateId);

	List<PaymentEvent> findByEventType(String eventType);
}
