package com.card_service.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.card_service.enums.CardNetwork;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CardNumberGenerator {
	private final SecureRandom random = new SecureRandom();

    public String generateCardNumber(CardNetwork network) {
        String prefix = getNetworkPrefix(network);
        String accountNumber = generateAccountNumber(15 - prefix.length());
        String cardNumber = prefix + accountNumber;
        
        // Add Luhn check digit
        int checkDigit = calculateLuhnCheckDigit(cardNumber);
        return cardNumber + checkDigit;
    }

    public String generateCVV() {
        return String.format("%03d", random.nextInt(1000));
    }

    public String generateActivationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    private String getNetworkPrefix(CardNetwork network) {
        return switch (network) {
            case VISA -> "4";
            case MASTERCARD -> "5" + (1 + random.nextInt(5)); // 51-55
            case RUPAY -> "60";
            case AMEX -> "34"; // or "37"
            case DINERS -> "36";
            case DISCOVER -> "6011";
        };
    }

    private String generateAccountNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private int calculateLuhnCheckDigit(String cardNumber) {
        int sum = 0;
        boolean alternate = true;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (10 - (sum % 10)) % 10;
    }

    public boolean validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }
        
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }
}
