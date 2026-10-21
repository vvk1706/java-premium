package com.dlis.licensing.dao.jdbc;

import com.dlis.licensing.domain.DrivingHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DrivingHistoryDaoImpl (package-private class in SupportingDaoImpls).
 * All JDBC collaborators are mocked — no DB2 connection required.
 */
@ExtendWith(MockitoExtension.class)
class DrivingHistoryDaoImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private DrivingHistoryDaoImpl dao;

    // ── findActiveByCandidateId ───────────────────────────────────────────────

    @Test
    void findActiveByCandidateId_oneRow_returnsOneRecord() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(true, false);
        stubResultSetForHistory();

        List<DrivingHistory> list = dao.findActiveByCandidateId(10L);

        assertEquals(1, list.size());
        verify(preparedStatement).setLong(1, 10L);
    }

    @Test
    void findActiveByCandidateId_multipleRows_returnsAllRecords() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(true, true, true, false);
        stubResultSetForHistory();

        List<DrivingHistory> list = dao.findActiveByCandidateId(10L);

        assertEquals(3, list.size());
    }

    @Test
    void findActiveByCandidateId_noRows_returnsEmptyList() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(false);

        List<DrivingHistory> list = dao.findActiveByCandidateId(10L);

        assertTrue(list.isEmpty());
        verify(preparedStatement).setLong(1, 10L);
    }

    @Test
    void findActiveByCandidateId_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dao.findActiveByCandidateId(10L));

        assertTrue(ex.getMessage().contains("DrivingHistoryDao.findActiveByCandidateId failed"));
    }

    // ── row mapping ───────────────────────────────────────────────────────────

    @Test
    void findActiveByCandidateId_rowMapping_mapsAllFields() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(true, false);
        stubResultSetForHistory();

        DrivingHistory h = dao.findActiveByCandidateId(10L).get(0);

        assertEquals(1L, h.getHistoryId());
        assertEquals(10L, h.getCandidateId());
        assertEquals(LocalDate.of(2023, 3, 15), h.getIncidentDate());
        assertEquals("OF", h.getIncidentType());
        assertEquals("Speeding", h.getIncidentDescription());
        assertEquals(3, h.getDemeritPoints());
        assertEquals(new BigDecimal("500.00"), h.getFineAmount());
        assertEquals("N", h.getFinePaidStatus());
        assertEquals(LocalDate.of(2023, 3, 15), h.getSuspensionStartDate());
        assertEquals(LocalDate.of(2023, 9, 15), h.getSuspensionEndDate());
        assertEquals("CC12345", h.getCourtCaseNumber());
        assertEquals("JMPD", h.getRecordedByAuthority());
        assertEquals("A", h.getRecordStatus());
    }

    @Test
    void findActiveByCandidateId_nullFineAmount_mappedAsZero() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(true, false);
        stubResultSetForHistory();
        // Simulate wasNull() returning true for FINE_AMOUNT
        when(resultSet.getBigDecimal("FINE_AMOUNT")).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(true);

        DrivingHistory h = dao.findActiveByCandidateId(10L).get(0);

        assertEquals(BigDecimal.ZERO, h.getFineAmount());
    }

    @Test
    void findActiveByCandidateId_nullSuspensionDates_mappedAsNull() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(true, false);
        stubResultSetForHistory();
        when(resultSet.getDate("SUSPENSION_START_DATE")).thenReturn(null);
        when(resultSet.getDate("SUSPENSION_END_DATE")).thenReturn(null);

        DrivingHistory h = dao.findActiveByCandidateId(10L).get(0);

        assertNull(h.getSuspensionStartDate());
        assertNull(h.getSuspensionEndDate());
    }

    @Test
    void findActiveByCandidateId_nonNullFineAmount_mappedFromResultSet() throws SQLException {
        stubQuery();
        when(resultSet.next()).thenReturn(true, false);
        stubResultSetForHistory();
        when(resultSet.getBigDecimal("FINE_AMOUNT")).thenReturn(new BigDecimal("150.00"));
        when(resultSet.wasNull()).thenReturn(false);

        DrivingHistory h = dao.findActiveByCandidateId(10L).get(0);

        assertEquals(new BigDecimal("150.00"), h.getFineAmount());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubQuery() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    private void stubResultSetForHistory() throws SQLException {
        when(resultSet.getLong("HISTORY_ID")).thenReturn(1L);
        when(resultSet.getLong("CANDIDATE_ID")).thenReturn(10L);
        when(resultSet.getDate("INCIDENT_DATE"))
                .thenReturn(Date.valueOf(LocalDate.of(2023, 3, 15)));
        when(resultSet.getString("INCIDENT_TYPE")).thenReturn("OF");
        when(resultSet.getString("INCIDENT_DESCRIPTION")).thenReturn("Speeding");
        when(resultSet.getInt("DEMERIT_POINTS")).thenReturn(3);
        when(resultSet.getBigDecimal("FINE_AMOUNT")).thenReturn(new BigDecimal("500.00"));
        when(resultSet.wasNull()).thenReturn(false);
        when(resultSet.getString("FINE_PAID_STATUS")).thenReturn("N");
        when(resultSet.getDate("SUSPENSION_START_DATE"))
                .thenReturn(Date.valueOf(LocalDate.of(2023, 3, 15)));
        when(resultSet.getDate("SUSPENSION_END_DATE"))
                .thenReturn(Date.valueOf(LocalDate.of(2023, 9, 15)));
        when(resultSet.getString("COURT_CASE_NUMBER")).thenReturn("CC12345");
        when(resultSet.getString("RECORDED_BY_AUTHORITY")).thenReturn("JMPD");
        when(resultSet.getString("RECORD_STATUS")).thenReturn("A");
    }
}
