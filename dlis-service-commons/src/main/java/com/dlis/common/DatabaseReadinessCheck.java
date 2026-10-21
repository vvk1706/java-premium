package com.dlis.common;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Kubernetes readiness probe: the service can obtain a valid DB2 connection.
 * While DB2 is unreachable the pod is removed from the Service endpoints,
 * but it is not restarted.
 */
@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    static final int VALIDATION_TIMEOUT_SECONDS = 2;

    @Resource(lookup = "jdbc/dlisDS")
    private DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(VALIDATION_TIMEOUT_SECONDS)
                    ? HealthCheckResponse.up("dlis-db2")
                    : HealthCheckResponse.down("dlis-db2");
        } catch (SQLException | RuntimeException e) {
            return HealthCheckResponse.named("dlis-db2").down()
                    .withData("error", e.getClass().getSimpleName()).build();
        }
    }
}
