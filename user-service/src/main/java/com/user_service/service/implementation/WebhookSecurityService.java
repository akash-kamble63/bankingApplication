package com.user_service.service.implementation;

import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;

@Slf4j
@Service
public class WebhookSecurityService {
    @Value("${webhook.keycloak.secret}")
    private String keycloakWebhookSecret;

    /**
     * Validates the webhook secret using constant-time comparison
     * to prevent timing attacks
     * 
     * @param providedSecret the secret from the webhook header
     * @return true if valid, false otherwise
     */
    public boolean validateWebhookSecret(String providedSecret) {
        if (!StringUtils.hasText(providedSecret)) {
            log.warn("Webhook secret is missing");
            return false;
        }

        if (!StringUtils.hasText(keycloakWebhookSecret)) {
            log.error("Webhook secret not configured in application properties");
            return false;
        }

        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                providedSecret.getBytes(),
                keycloakWebhookSecret.getBytes());
    }

    /**
     * Alternative: Validates HMAC signature if Keycloak sends signed payloads
     * Use this if Keycloak sends a signature instead of plain secret
     * 
     * @param payload   the raw request body
     * @param signature the signature from webhook header
     * @return true if signature is valid, false otherwise
     */
    public boolean validateHmacSignature(String payload, String signature) {
        if (!StringUtils.hasText(payload) || !StringUtils.hasText(signature)) {
            log.warn("Payload or signature is missing");
            return false;
        }

        try {
            String computedSignature = computeHmacSha256(payload);
            return MessageDigest.isEqual(
                    signature.getBytes(),
                    computedSignature.getBytes());
        } catch (Exception e) {
            log.error("Error validating HMAC signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Computes HMAC-SHA256 signature for the payload
     */
    private String computeHmacSha256(String payload) throws NoSuchAlgorithmException {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    keycloakWebhookSecret.getBytes(),
                    "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }
}
