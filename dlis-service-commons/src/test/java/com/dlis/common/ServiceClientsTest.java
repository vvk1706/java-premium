package com.dlis.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServiceClientsTest {

    @Test
    void baseUrl_usesEnvAndStripsTrailingSlash() {
        Map<String, String> env = Map.of("X_URL", " http://svc:9080/dlis/api/v1/ ");
        assertEquals("http://svc:9080/dlis/api/v1", ServiceClients.baseUrl(env::get, "X_URL", "http://d"));
    }

    @Test
    void baseUrl_fallsBackToDefaultWhenUnsetOrBlank() {
        assertEquals("http://d", ServiceClients.baseUrl(k -> null, "X_URL", "http://d"));
        assertEquals("http://d", ServiceClients.baseUrl(k -> "  ", "X_URL", "http://d"));
    }

    @Test
    void authHeader_isBasicBase64() {
        Map<String, String> env = Map.of("DLIS_SERVICE_USER", "svc", "DLIS_SERVICE_PASSWORD", "pw");
        assertEquals("Basic c3ZjOnB3", ServiceClients.authHeader(env::get));
    }

    @Test
    void authHeader_failsWhenCredentialsMissing() {
        assertThrows(IllegalStateException.class, () -> ServiceClients.authHeader(k -> null));
    }
}
