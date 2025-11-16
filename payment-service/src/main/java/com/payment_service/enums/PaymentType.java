package com.payment_service.enums;

public enum PaymentType {
	CARD_PAYMENT,       // Online card payment
    BILL_PAYMENT,       // Utility bills
    P2P_TRANSFER,       // Person to person
    MERCHANT_PAYMENT,   // Payment to merchant
    SUBSCRIPTION,       // Recurring subscription
    REFUND,             // Refund transaction
    TOP_UP              // Wallet top-up
}
