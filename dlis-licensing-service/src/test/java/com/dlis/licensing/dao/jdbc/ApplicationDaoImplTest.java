package com.dlis.licensing.dao.jdbc;

import com.dlis.licensing.domain.LicenseApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApplicationDaoImpl.
 * All JDBC collaborators are mocked — no DB2 connection required.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationDaoImplTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSet generatedKeys;

    @InjectMocks
    private ApplicationDaoImpl dao;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
    }

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    void insert_success_setsGeneratedKey() throws SQLException {
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getLong(1)).thenReturn(100L);

        LicenseApplication app = buildApplication();

        dao.insert(app);

        verify(preparedStatement).executeUpdate();
        assertEquals(100L, app.getApplicationId());
    }

    @Test
    void insert_noGeneratedKey_applicationIdUnchanged() throws SQLException {
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false);

        LicenseApplication app = buildApplication();

        dao.insert(app);

        assertNull(app.getApplicationId());
    }

    @Test
    void insert_bindsParametersInCorrectOrder() throws SQLException {
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false);

        LicenseApplication app = buildApplication();

        dao.insert(app);

        verify(preparedStatement).setLong(1, 55L);              // candidateId
        verify(preparedStatement).setString(2, "L");            // licenseType
        verify(preparedStatement).setString(4, LicenseApplication.STATUS_PENDING);
        verify(preparedStatement).setString(5, LicenseApplication.CHECK_UNCHECKED);
        verify(preparedStatement).setString(6, LicenseApplication.CHECK_UNCHECKED);
        verify(preparedStatement).setString(7, LicenseApplication.PAYMENT_UNPAID);
        verify(preparedStatement).setString(8, LicenseApplication.CHECK_UNCHECKED);
        verify(preparedStatement).setString(9, LicenseApplication.CHECK_UNCHECKED);
        verify(preparedStatement).setString(12, "OFFICER1");    // createdBy
        verify(preparedStatement).setString(13, "OFFICER1");    // lastUpdatedBy
    }

    @Test
    void insert_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dao.insert(buildApplication()));

        assertTrue(ex.getMessage().contains("ApplicationDao.insert failed"));
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_existingId_returnsApplication() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        stubResultSetForApplication();

        Optional<LicenseApplication> result = dao.findById(100L);

        assertTrue(result.isPresent());
        assertEquals(100L, result.get().getApplicationId());
        assertEquals(55L, result.get().getCandidateId());
        assertEquals("L", result.get().getLicenseType());
        verify(preparedStatement).setLong(1, 100L);
    }

    @Test
    void findById_missingId_returnsEmpty() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<LicenseApplication> result = dao.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findById_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.findById(1L));

        assertTrue(ex.getMessage().contains("ApplicationDao.findById failed"));
    }

    @Test
    void findById_nullableDates_mappedAsNull() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        stubResultSetForApplication();
        // Override nullable date columns to null
        when(resultSet.getDate("ELIGIBILITY_CHK_DATE")).thenReturn(null);
        when(resultSet.getDate("HISTORY_CHK_DATE")).thenReturn(null);
        when(resultSet.getDate("APPROVAL_1_DATE")).thenReturn(null);
        when(resultSet.getDate("APPROVAL_2_DATE")).thenReturn(null);

        LicenseApplication app = dao.findById(100L).get();

        assertNull(app.getEligibilityChkDate());
        assertNull(app.getHistoryChkDate());
        assertNull(app.getApproval1Date());
        assertNull(app.getApproval2Date());
    }

    // ── findActiveByCandidate ─────────────────────────────────────────────────

    @Test
    void findActiveByCandidate_twoRows_returnsTwoApplications() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        stubResultSetForApplication();

        List<LicenseApplication> list = dao.findActiveByCandidate(55L, "L");

        assertEquals(2, list.size());
        verify(preparedStatement).setLong(1, 55L);
        verify(preparedStatement).setString(2, "L");
    }

    @Test
    void findActiveByCandidate_noRows_returnsEmptyList() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<LicenseApplication> list = dao.findActiveByCandidate(55L, "L");

        assertTrue(list.isEmpty());
    }

    @Test
    void findActiveByCandidate_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dao.findActiveByCandidate(55L, "L"));

        assertTrue(ex.getMessage().contains("ApplicationDao.findActiveByCandidate failed"));
    }

    // ── updateEligibilityCheck ────────────────────────────────────────────────

    @Test
    void updateEligibilityCheck_success_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updateEligibilityCheck(100L, "P", LocalDate.of(2024, 6, 1),
                "OK", "EC", LocalDate.of(2024, 6, 1), "OFFICER1");

        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).setObject(1, "P");
        verify(preparedStatement).setObject(7, 100L);
    }

    @Test
    void updateEligibilityCheck_sqlException_throwsRuntimeException() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dao.updateEligibilityCheck(1L, "P", LocalDate.now(),
                        "note", "EC", LocalDate.now(), "USR"));

        assertTrue(ex.getMessage().contains("ApplicationDao update failed"));
    }

    // ── updateHistoryCheck ────────────────────────────────────────────────────

    @Test
    void updateHistoryCheck_success_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updateHistoryCheck(100L, "P", LocalDate.of(2024, 6, 1),
                "Cleared", "HC", LocalDate.of(2024, 6, 1), "OFFICER1");

        verify(preparedStatement).executeUpdate();
    }

    // ── updatePayment ─────────────────────────────────────────────────────────

    @Test
    void updatePayment_success_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updatePayment(100L, "P", "RCP20240601100", "PA",
                LocalDate.of(2024, 6, 1), "OFFICER1");

        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).setObject(1, "P");
        verify(preparedStatement).setObject(2, "RCP20240601100");
    }

    // ── updateApproval1 ───────────────────────────────────────────────────────

    @Test
    void updateApproval1_success_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updateApproval1(100L, "A", "AUTH1", LocalDate.of(2024, 6, 1),
                "All good", "A1", LocalDate.of(2024, 6, 1), "OFFICER1");

        verify(preparedStatement).executeUpdate();
    }

    // ── updateApproval2 ───────────────────────────────────────────────────────

    @Test
    void updateApproval2_success_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updateApproval2(100L, "A", "AUTH2", LocalDate.of(2024, 6, 1),
                "Approved", "A2", LocalDate.of(2024, 6, 1), "OFFICER2");

        verify(preparedStatement).executeUpdate();
    }

    // ── updateApplicationStatus ───────────────────────────────────────────────

    @Test
    void updateApplicationStatus_success_executesUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updateApplicationStatus(100L, "IS", LocalDate.of(2024, 6, 1), "SYS");

        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).setObject(1, "IS");
        verify(preparedStatement).setObject(4, 100L);
    }

    // ── row mapping ───────────────────────────────────────────────────────────

    @Test
    void findById_rowMapping_mapsAllFields() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        stubResultSetForApplication();

        LicenseApplication app = dao.findById(100L).get();

        assertEquals(100L, app.getApplicationId());
        assertEquals(55L, app.getCandidateId());
        assertEquals("L", app.getLicenseType());
        assertEquals(LocalDate.of(2024, 1, 10), app.getApplicationDate());
        assertEquals("PE", app.getApplicationStatus());
        assertEquals("U", app.getEligibilityChkStatus());
        assertEquals("U", app.getHistoryChkStatus());
        assertEquals("U", app.getPaymentStatus());
        assertNull(app.getPaymentReference());
        assertEquals("U", app.getApproval1Status());
        assertNull(app.getApproval1Authority());
        assertEquals("U", app.getApproval2Status());
        assertNull(app.getRejectionReason());
        assertEquals(LocalDate.of(2024, 1, 10), app.getCreatedDate());
        assertEquals("OFFICER1", app.getCreatedBy());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private LicenseApplication buildApplication() {
        LicenseApplication app = new LicenseApplication();
        app.setCandidateId(55L);
        app.setLicenseType("L");
        app.setCreatedBy("OFFICER1");
        return app;
    }

    private void stubResultSetForApplication() throws SQLException {
        LocalDate date = LocalDate.of(2024, 1, 10);
        when(resultSet.getLong("APPLICATION_ID")).thenReturn(100L);
        when(resultSet.getLong("CANDIDATE_ID")).thenReturn(55L);
        when(resultSet.getString("LICENSE_TYPE")).thenReturn("L");
        when(resultSet.getDate("APPLICATION_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getString("APPLICATION_STATUS")).thenReturn("PE");
        when(resultSet.getString("ELIGIBILITY_CHK_STATUS")).thenReturn("U");
        when(resultSet.getDate("ELIGIBILITY_CHK_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getString("ELIGIBILITY_CHK_NOTES")).thenReturn(null);
        when(resultSet.getString("HISTORY_CHK_STATUS")).thenReturn("U");
        when(resultSet.getDate("HISTORY_CHK_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getString("HISTORY_CHK_NOTES")).thenReturn(null);
        when(resultSet.getString("PAYMENT_STATUS")).thenReturn("U");
        when(resultSet.getString("PAYMENT_REFERENCE")).thenReturn(null);
        when(resultSet.getString("APPROVAL_1_STATUS")).thenReturn("U");
        when(resultSet.getString("APPROVAL_1_AUTHORITY")).thenReturn(null);
        when(resultSet.getDate("APPROVAL_1_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getString("APPROVAL_1_NOTES")).thenReturn(null);
        when(resultSet.getString("APPROVAL_2_STATUS")).thenReturn("U");
        when(resultSet.getString("APPROVAL_2_AUTHORITY")).thenReturn(null);
        when(resultSet.getDate("APPROVAL_2_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getString("APPROVAL_2_NOTES")).thenReturn(null);
        when(resultSet.getString("REJECTION_REASON")).thenReturn(null);
        when(resultSet.getDate("CREATED_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getDate("LAST_UPDATED_DATE")).thenReturn(Date.valueOf(date));
        when(resultSet.getString("CREATED_BY")).thenReturn("OFFICER1");
        when(resultSet.getString("LAST_UPDATED_BY")).thenReturn("OFFICER1");
    }
}
