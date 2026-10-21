package com.dlis.common;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Support for synchronous service-to-service REST calls inside the cluster.
 * Base URLs and the service credential come from the environment (ConfigMap / Secret).
 */
public final class ServiceClients {

    public static final int CONNECT_TIMEOUT_MS = 2000;
    public static final int READ_TIMEOUT_MS    = 5000;

    private ServiceClients() {}

    /** JAX-RS client with bounded connect/read timeouts so a slow peer cannot exhaust the pool. */
    public static Client newClient() {
        return ClientBuilder.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .build();
    }

    public static String baseUrl(String envName, String defaultUrl) {
        return baseUrl(System::getenv, envName, defaultUrl);
    }

    static String baseUrl(Function<String, String> env, String envName, String defaultUrl) {
        String v = env.apply(envName);
        String url = (v == null || v.isBlank()) ? defaultUrl : v.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** HTTP Basic header for the internal DLIS_SERVICE account. */
    public static String authHeader() {
        return authHeader(System::getenv);
    }

    static String authHeader(Function<String, String> env) {
        String user = env.apply("DLIS_SERVICE_USER");
        String pass = env.apply("DLIS_SERVICE_PASSWORD");
        if (user == null || pass == null) {
            throw new IllegalStateException("DLIS_SERVICE_USER / DLIS_SERVICE_PASSWORD are not set");
        }
        String token = Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
