package com.user_service.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KeycloakHealthIndicator implements HealthIndicator {

    private final KeyCloakProp keycloakProp;

    @Override
    public Health health() {
        try {
            keycloakProp.getKeycloakInstance()
                        .serverInfo()
                        .getInfo();

            return Health.up()
                    .withDetail("realm", keycloakProp.getRealm())
                    .withDetail("serverUrl", keycloakProp.getServerUrl())
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
