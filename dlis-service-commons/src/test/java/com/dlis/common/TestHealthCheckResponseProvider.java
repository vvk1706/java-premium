package com.dlis.common;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Minimal MicroProfile Health SPI implementation so checks can be unit tested without Liberty. */
public class TestHealthCheckResponseProvider implements HealthCheckResponseProvider {

    @Override
    public HealthCheckResponseBuilder createResponseBuilder() {
        return new HealthCheckResponseBuilder() {
            private String name;
            private HealthCheckResponse.Status status = HealthCheckResponse.Status.UP;
            private final Map<String, Object> data = new LinkedHashMap<>();

            @Override public HealthCheckResponseBuilder name(String n) { this.name = n; return this; }
            @Override public HealthCheckResponseBuilder withData(String k, String v) { data.put(k, v); return this; }
            @Override public HealthCheckResponseBuilder withData(String k, long v) { data.put(k, v); return this; }
            @Override public HealthCheckResponseBuilder withData(String k, boolean v) { data.put(k, v); return this; }
            @Override public HealthCheckResponseBuilder up() { status = HealthCheckResponse.Status.UP; return this; }
            @Override public HealthCheckResponseBuilder down() { status = HealthCheckResponse.Status.DOWN; return this; }
            @Override public HealthCheckResponseBuilder status(boolean up) {
                status = up ? HealthCheckResponse.Status.UP : HealthCheckResponse.Status.DOWN; return this;
            }
            @Override public HealthCheckResponse build() {
                final String n = name; final HealthCheckResponse.Status s = status;
                final Map<String, Object> d = new LinkedHashMap<>(data);
                return new HealthCheckResponse() {
                    @Override public String getName() { return n; }
                    @Override public Status getStatus() { return s; }
                    @Override public Optional<Map<String, Object>> getData() { return Optional.of(d); }
                };
            }
        };
    }
}
