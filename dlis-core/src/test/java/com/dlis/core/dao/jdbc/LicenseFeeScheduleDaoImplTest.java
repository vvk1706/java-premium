package com.dlis.core.dao.jdbc;

import com.dlis.core.domain.LicenseFeeSchedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LicenseFeeScheduleDaoImpl using mocked JDBC objects.
 */
@ExtendWith(MockitoExtension.class)
class LicenseFeeScheduleDaoImplTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @InjectMocks
    private LicenseFeeScheduleDaoImpl dao;

    // ── findActiveFee ─────────────────────────────────────────────────────────

    @Test
    void findActiveFee_rowWithExpiry_returnsMappedFee() throws SQLException {
        stubQuery();
        stubRow(Date.valueOf(LocalDate.of(2030, 12, 31)));

        Optional<LicenseFeeSchedule> result = dao.findActiveFee("L", "APP");

        assertTrue(result.isPresent());
        LicenseFeeSchedule f = result.get();
        assertEquals(1L, f.getFeeScheduleId());
        assertEquals("L", f.getLicenseType());
        assertEquals("APP", f.getFeeType());
        assertEquals(new BigDecimal("250.00"), f.getFeeAmount());
        assertEquals(LocalDate.of(2020, 1, 1), f.getEffectiveDate());
        assertEquals(LocalDate.of(2030, 12, 31), f.getExpiryDate());
        assertEquals("ZAR", f.getCurrencyCode());
        assertEquals("Application fee", f.getDescription());
        assertEquals("A", f.getActiveStatus());
        verify(ps).setString(1, "L");
        verify(ps).setString(2, "APP");
    }

    @Test
    void findActiveFee_nullExpiryDate_mapsExpiryToNull() throws SQLException {
        stubQuery();
        stubRow(null);

        Optional<LicenseFeeSchedule> result = dao.findActiveFee("L", "APP");

        assertTrue(result.isPresent());
        assertNull(result.get().getExpiryDate());
    }

    @Test
    void findActiveFee_noRow_returnsEmpty() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(false);

        Optional<LicenseFeeSchedule> result = dao.findActiveFee("O", "APP");

        assertTrue(result.isEmpty());
        verify(ps).setString(1, "O");
        verify(ps).setString(2, "APP");
    }

    @Test
    void findActiveFee_sqlException_throwsRuntimeException() throws SQLException {
        SQLException cause = new SQLException("down");
        when(dataSource.getConnection()).thenThrow(cause);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> dao.findActiveFee("L", "APP"));

        assertEquals("LicenseFeeScheduleDao.findActiveFee failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubQuery() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
    }

    private void stubRow(Date expiry) throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getLong("FEE_SCHEDULE_ID")).thenReturn(1L);
        when(rs.getString("LICENSE_TYPE")).thenReturn("L");
        when(rs.getString("FEE_TYPE")).thenReturn("APP");
        when(rs.getBigDecimal("FEE_AMOUNT")).thenReturn(new BigDecimal("250.00"));
        when(rs.getDate("EFFECTIVE_DATE")).thenReturn(Date.valueOf(LocalDate.of(2020, 1, 1)));
        when(rs.getDate("EXPIRY_DATE")).thenReturn(expiry);
        when(rs.getString("CURRENCY_CODE")).thenReturn("ZAR");
        when(rs.getString("DESCRIPTION")).thenReturn("Application fee");
        when(rs.getString("ACTIVE_STATUS")).thenReturn("A");
    }
}
