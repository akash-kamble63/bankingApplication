package com.card_service.service;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EncryptionService {
	private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    @Value("${card.encryption.key:32CharacterSecureEncryptionKey}")
    private String encryptionKey;

    public String encrypt(String plainText) {
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            
            return Base64.getEncoder().encodeToString(byteBuffer.array());
            
        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
            
        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public String hashPin(String pin) {
        // Use BCrypt for PIN hashing (would need BCrypt library)
        // For now, using SHA-256 (in production, use BCrypt)
        return hash(pin + encryptionKey);
    }

    public boolean verifyPin(String rawPin, String hashedPin) {
        return hashPin(rawPin).equals(hashedPin);
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = encryptionKey.getBytes();
        return new SecretKeySpec(keyBytes, 0, 32, "AES");
    }
}
