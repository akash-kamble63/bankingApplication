package com.transaction_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transaction_service.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}
