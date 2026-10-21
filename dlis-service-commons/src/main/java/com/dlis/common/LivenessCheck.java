package com.dlis.common;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;

/**
 * Kubernetes liveness probe: the JVM and the JAX-RS runtime are able to answer.
 * Deliberately does NOT touch the database, so a DB2 outage never restarts pods.
 */
@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("dlis-live");
    }
}
