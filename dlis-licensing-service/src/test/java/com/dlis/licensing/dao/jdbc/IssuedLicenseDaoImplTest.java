package com.dlis.licensing.dao.jdbc;

import com.dlis.licensing.domain.IssuedLicense;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IssuedLicenseDaoImpl using mocked JDBC objects.
 */
@ExtendWith(MockitoExtension.class)
class IssuedLicenseDaoImplTest {

    private static final LocalDate ISSUE = LocalDate.of(2024, 1, 10);
    private static final LocalDate EXPIRY = LocalDate.of(2029, 1, 10);

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @InjectMocks
    private IssuedLicenseDaoImpl dao;

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    void insert_fullLicense_bindsParametersInOrderAndSetsGeneratedKey() throws SQLException {
        IssuedLicense lic = license();
        lic.setDemeritBalance(8);
        lic.setRenewalCount(2);
        stubInsert();
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(77L);

        dao.insert(lic);

        verifyInsertBindings(8, 2);
        verify(ps).executeUpdate();
        assertEquals(77L, lic.getLicenseId());
    }

    @Test
    void insert_nullDemeritAndRenewal_usesDefaults12And0() throws SQLException {
        IssuedLicense lic = license();
        lic.setDemeritBalance(null);
        lic.setRenewalCount(null);
        stubInsert();
        when(rs.next()).thenReturn(false);

        dao.insert(lic);

        verifyInsertBindings(12, 0);
        assertNull(lic.getLicenseId());
    }

    @Test
    void insert_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.insert(license()));

        assertEquals("IssuedLicenseDao.insert failed", ex.getMessage());
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_rowExists_returnsMappedLicense() throws SQLException {
        stubQuery();
        stubRow(true);

        Optional<IssuedLicense> result = dao.findById(3L);

        assertTrue(result.isPresent());
        IssuedLicense l = result.get();
        assertEquals(3L, l.getLicenseId());
        assertEquals(10L, l.getApplicationId());
        assertEquals(5L, l.getCandidateId());
        assertEquals("DL0001", l.getLicenseNumber());
        assertEquals("P", l.getLicenseType());
        assertEquals(ISSUE, l.getIssueDate());
        assertEquals(EXPIRY, l.getExpiryDate());
        assertEquals("A", l.getLicenseStatus());
        assertEquals("B", l.getVehicleClass());
        assertEquals("None", l.getRestrictions());
        assertEquals(12, l.getDemeritBalance());
        assertEquals("DOT", l.getIssuedByAuthority());
        assertEquals("officer1", l.getIssuedByOfficer());
        assertEquals(1, l.getRenewalCount());
        assertEquals(2L, l.getPreviousLicenseId());
        assertEquals("note", l.getNotes());
        verify(ps).setLong(1, 3L);
    }

    @Test
    void findById_previousLicenseIdNull_mapsToNull() throws SQLException {
        stubQuery();
        stubRow(false);

        Optional<IssuedLicense> result = dao.findById(3L);

        assertTrue(result.isPresent());
        assertNull(result.get().getPreviousLicenseId());
    }

    @Test
    void findById_noRow_returnsEmpty() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findById(3L).isEmpty());
    }

    @Test
    void findById_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.findById(3L));

        assertEquals("IssuedLicenseDao.findById failed", ex.getMessage());
    }

    // ── findByLicenseNumber ───────────────────────────────────────────────────

    @Test
    void findByLicenseNumber_rowExists_returnsLicense() throws SQLException {
        stubQuery();
        stubRow(true);

        Optional<IssuedLicense> result = dao.findByLicenseNumber("DL0001");

        assertTrue(result.isPresent());
        assertEquals("DL0001", result.get().getLicenseNumber());
        verify(ps).setString(1, "DL0001");
    }

    @Test
    void findByLicenseNumber_noRow_returnsEmpty() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findByLicenseNumber("NOPE").isEmpty());
        verify(ps).setString(1, "NOPE");
    }

    @Test
    void findByLicenseNumber_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> dao.findByLicenseNumber("DL0001"));

        assertEquals("IssuedLicenseDao.findByLicenseNumber failed", ex.getMessage());
    }

    // ── findActiveByCandidate ─────────────────────────────────────────────────

    @Test
    void findActiveByCandidate_multipleRows_returnsAllAndBindsParameters() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getLong("LICENSE_ID")).thenReturn(1L, 2L);
        when(rs.getDate("ISSUE_DATE")).thenReturn(Date.valueOf(ISSUE));
        when(rs.getDate("EXPIRY_DATE")).thenReturn(Date.valueOf(EXPIRY));

        List<IssuedLicense> result = dao.findActiveByCandidate(5L, "P");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getLicenseId());
        assertEquals(2L, result.get(1).getLicenseId());
        verify(ps).setLong(1, 5L);
        verify(ps).setString(2, "P");
    }

    @Test
    void findActiveByCandidate_noRows_returnsEmptyList() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(false);

        assertTrue(dao.findActiveByCandidate(5L, "P").isEmpty());
    }

    @Test
    void findActiveByCandidate_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> dao.findActiveByCandidate(5L, "P"));

        assertEquals("IssuedLicenseDao.findActiveByCandidate failed", ex.getMessage());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubInsert() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
    }

    private void stubQuery() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
    }

    private void verifyInsertBindings(int demerit, int renewals) throws SQLException {
        verify(ps).setLong(1, 10L);
        verify(ps).setLong(2, 5L);
        verify(ps).setString(3, "DL0001");
        verify(ps).setString(4, "P");
        verify(ps).setDate(5, Date.valueOf(ISSUE));
        verify(ps).setDate(6, Date.valueOf(EXPIRY));
        verify(ps).setString(7, "A");
        verify(ps).setString(8, "B");
        verify(ps).setString(9, "None");
        verify(ps).setInt(10, demerit);
        verify(ps).setString(11, "DOT");
        verify(ps).setString(12, "officer1");
        verify(ps).setInt(13, renewals);
    }

    private void stubRow(boolean hasPrevious) throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getLong("LICENSE_ID")).thenReturn(3L);
        when(rs.getLong("APPLICATION_ID")).thenReturn(10L);
        when(rs.getLong("CANDIDATE_ID")).thenReturn(5L);
        when(rs.getString("LICENSE_NUMBER")).thenReturn("DL0001");
        when(rs.getString("LICENSE_TYPE")).thenReturn("P");
        when(rs.getDate("ISSUE_DATE")).thenReturn(Date.valueOf(ISSUE));
        when(rs.getDate("EXPIRY_DATE")).thenReturn(Date.valueOf(EXPIRY));
        when(rs.getString("LICENSE_STATUS")).thenReturn("A");
        when(rs.getString("VEHICLE_CLASS")).thenReturn("B");
        when(rs.getString("RESTRICTIONS")).thenReturn("None");
        when(rs.getInt("DEMERIT_BALANCE")).thenReturn(12);
        when(rs.getString("ISSUED_BY_AUTHORITY")).thenReturn("DOT");
        when(rs.getString("ISSUED_BY_OFFICER")).thenReturn("officer1");
        when(rs.getInt("RENEWAL_COUNT")).thenReturn(1);
        when(rs.getLong("PREVIOUS_LICENSE_ID")).thenReturn(hasPrevious ? 2L : 0L);
        when(rs.wasNull()).thenReturn(!hasPrevious);
        when(rs.getString("NOTES")).thenReturn("note");
    }

    private IssuedLicense license() {
        IssuedLicense l = new IssuedLicense();
        l.setApplicationId(10L);
        l.setCandidateId(5L);
        l.setLicenseNumber("DL0001");
        l.setLicenseType("P");
        l.setIssueDate(ISSUE);
        l.setExpiryDate(EXPIRY);
        l.setLicenseStatus("A");
        l.setVehicleClass("B");
        l.setRestrictions("None");
        l.setIssuedByAuthority("DOT");
        l.setIssuedByOfficer("officer1");
        return l;
    }
}
