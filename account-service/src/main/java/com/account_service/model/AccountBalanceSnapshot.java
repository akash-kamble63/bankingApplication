package com.account_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Entity
@Table(name = "account_balance_snapshots", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_snapshot_date", columnList = "snapshot_date")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountBalanceSnapshot { // for history

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;
    
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate; // Daily EOD snapshot
    
    @Column(name = "snapshot_type", length = 20)
    private String snapshotType; // DAILY, MONTHLY, YEARLY
}
