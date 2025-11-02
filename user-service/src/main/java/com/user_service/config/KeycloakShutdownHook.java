package com.user_service.config;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakShutdownHook implements ApplicationListener<ContextClosedEvent> {
    private final KeyCloakProp keyCloakProp;

	
	@Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Application shutting down, closing Keycloak connections");
        keyCloakProp.closeKeycloakInstance();
    }
}
