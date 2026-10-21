package com.dlis.common;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HealthChecksTest {

    private static DatabaseReadinessCheck withDataSource(DataSource ds) throws Exception {
        DatabaseReadinessCheck check = new DatabaseReadinessCheck();
        Field f = DatabaseReadinessCheck.class.getDeclaredField("dataSource");
        f.setAccessible(true);
        f.set(check, ds);
        return check;
    }

    @Test
    void liveness_isAlwaysUp() {
        assertEquals(HealthCheckResponse.Status.UP, new LivenessCheck().call().getStatus());
    }

    @Test
    void readiness_upWhenConnectionValid() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.isValid(DatabaseReadinessCheck.VALIDATION_TIMEOUT_SECONDS)).thenReturn(true);

        assertEquals(HealthCheckResponse.Status.UP, withDataSource(ds).call().getStatus());
        verify(c).close();
    }

    @Test
    void readiness_downWhenConnectionInvalid() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection c = mock(Connection.class);
        when(ds.getConnection()).thenReturn(c);
        when(c.isValid(anyInt())).thenReturn(false);

        assertEquals(HealthCheckResponse.Status.DOWN, withDataSource(ds).call().getStatus());
    }

    @Test
    void readiness_downWhenDatabaseUnreachable() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("down"));

        assertEquals(HealthCheckResponse.Status.DOWN, withDataSource(ds).call().getStatus());
    }
}
