package com.transaction_service.enums;

public enum TransactionType {
	TRANSFER,           // Account to account
    DEPOSIT,            // Cash/check deposit
    WITHDRAWAL,         // ATM/cash withdrawal
    BILL_PAYMENT,       // Utility bills
    CARD_PAYMENT,       // Card transaction
    REFUND,             // Refund transaction
    REVERSAL            // Transaction reversal
}
