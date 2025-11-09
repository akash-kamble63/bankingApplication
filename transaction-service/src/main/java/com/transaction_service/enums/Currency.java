package com.transaction_service.enums;

public enum Currency {
	INR("Indian Rupee", "₹", "INR"),
    USD("US Dollar", "$", "USD"),
    EUR("Euro", "€", "EUR"),
    GBP("British Pound", "£", "GBP"),
    JPY("Japanese Yen", "¥", "JPY"),
    AUD("Australian Dollar", "A$", "AUD"),
    CAD("Canadian Dollar", "C$", "CAD"),
    CHF("Swiss Franc", "CHF", "CHF"),
    CNY("Chinese Yuan", "¥", "CNY"),
    SGD("Singapore Dollar", "S$", "SGD"),
    AED("UAE Dirham", "AED", "AED");
    
    private final String fullName;
    private final String symbol;
    private final String code;
    
    Currency(String fullName, String symbol, String code) {
        this.fullName = fullName;
        this.symbol = symbol;
        this.code = code;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getCode() {
        return code;
    }
}
