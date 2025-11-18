package com.card_service.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TokenGenerator {
	private final SecureRandom random = new SecureRandom();

    public String generateCardToken() {
        // Generate PCI-DSS compliant token
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        
        String token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(tokenBytes);
        
        return "tok_card_" + token.substring(0, 32);
    }

    public String generateCardReference() {
        return "CARD-" + System.currentTimeMillis() + "-" + 
               String.format("%04d", random.nextInt(10000));
    }
}
