package com.dlis.candidate.dao.jdbc;

import com.dlis.candidate.domain.Candidate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CandidateDaoImpl using mocked JDBC objects.
 */
@ExtendWith(MockitoExtension.class)
class CandidateDaoImplTest {

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement ps;
    @Mock private ResultSet rs;

    @InjectMocks
    private CandidateDaoImpl dao;

    // ── insert ────────────────────────────────────────────────────────────────

    @Test
    void insert_validCandidate_bindsParametersInOrderAndSetsGeneratedKey() throws SQLException {
        Candidate c = candidate();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(42L);

        dao.insert(c);

        verify(ps).setString(1, "Jane");
        verify(ps).setString(2, "Smith");
        verify(ps).setDate(3, Date.valueOf(LocalDate.of(1990, 5, 15)));
        verify(ps).setString(4, "ID12345678");
        verify(ps).setString(5, "123 Main Street");
        verify(ps).setString(6, "Flat 4");
        verify(ps).setString(7, "Johannesburg");
        verify(ps).setString(8, "Gauteng");
        verify(ps).setString(9, "2000");
        verify(ps).setString(10, "ZA");
        verify(ps).setString(11, "0123456789");
        verify(ps).setString(12, "jane@example.com");
        verify(ps).setDate(13, Date.valueOf(LocalDate.now()));
        verify(ps).setString(14, "A");
        verify(ps).executeUpdate();
        assertEquals(42L, c.getCandidateId());
    }

    @Test
    void insert_noGeneratedKey_leavesCandidateIdUnchanged() throws SQLException {
        Candidate c = candidate();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.getGeneratedKeys()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        dao.insert(c);

        assertNull(c.getCandidateId());
        verify(rs, never()).getLong(1);
    }

    @Test
    void insert_sqlException_throwsRuntimeException() throws SQLException {
        SQLException cause = new SQLException("boom");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(ps);
        when(ps.executeUpdate()).thenThrow(cause);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.insert(candidate()));

        assertEquals("CandidateDao.insert failed", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_rowExists_returnsMappedCandidate() throws SQLException {
        stubQuery();
        stubRow();

        Optional<Candidate> result = dao.findById(7L);

        assertTrue(result.isPresent());
        Candidate c = result.get();
        assertEquals(7L, c.getCandidateId());
        assertEquals("Jane", c.getFirstName());
        assertEquals("Smith", c.getLastName());
        assertEquals(LocalDate.of(1990, 5, 15), c.getDateOfBirth());
        assertEquals("ID12345678", c.getIdNumber());
        assertEquals("123 Main Street", c.getAddressLine1());
        assertEquals("Flat 4", c.getAddressLine2());
        assertEquals("Johannesburg", c.getCity());
        assertEquals("Gauteng", c.getStateProvince());
        assertEquals("2000", c.getPostalCode());
        assertEquals("ZA", c.getCountry());
        assertEquals("0123456789", c.getPhoneNumber());
        assertEquals("jane@example.com", c.getEmailAddress());
        assertEquals(LocalDate.of(2020, 1, 2), c.getCreatedDate());
        assertEquals("A", c.getRecordStatus());
        verify(ps).setLong(1, 7L);
    }

    @Test
    void findById_noRow_returnsEmpty() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(false);

        Optional<Candidate> result = dao.findById(7L);

        assertTrue(result.isEmpty());
        verify(ps).setLong(1, 7L);
    }

    @Test
    void findById_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.findById(7L));

        assertEquals("CandidateDao.findById failed", ex.getMessage());
        assertInstanceOf(SQLException.class, ex.getCause());
    }

    // ── findByIdNumber ────────────────────────────────────────────────────────

    @Test
    void findByIdNumber_rowExists_returnsCandidate() throws SQLException {
        stubQuery();
        stubRow();

        Optional<Candidate> result = dao.findByIdNumber("ID12345678");

        assertTrue(result.isPresent());
        assertEquals("ID12345678", result.get().getIdNumber());
        verify(ps).setString(1, "ID12345678");
    }

    @Test
    void findByIdNumber_noRow_returnsEmpty() throws SQLException {
        stubQuery();
        when(rs.next()).thenReturn(false);

        Optional<Candidate> result = dao.findByIdNumber("UNKNOWN");

        assertTrue(result.isEmpty());
        verify(ps).setString(1, "UNKNOWN");
    }

    @Test
    void findByIdNumber_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.findByIdNumber("X"));

        assertEquals("CandidateDao.findByIdNumber failed", ex.getMessage());
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_validArgs_bindsStatusThenIdAndExecutes() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);

        dao.updateStatus(9L, "I");

        verify(ps).setString(1, "I");
        verify(ps).setLong(2, 9L);
        verify(ps).executeUpdate();
    }

    @Test
    void updateStatus_sqlException_throwsRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("bad sql"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.updateStatus(9L, "I"));

        assertEquals("CandidateDao.updateStatus failed", ex.getMessage());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubQuery() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
    }

    private void stubRow() throws SQLException {
        when(rs.next()).thenReturn(true);
        when(rs.getLong("CANDIDATE_ID")).thenReturn(7L);
        when(rs.getString("FIRST_NAME")).thenReturn("Jane");
        when(rs.getString("LAST_NAME")).thenReturn("Smith");
        when(rs.getDate("DATE_OF_BIRTH")).thenReturn(Date.valueOf(LocalDate.of(1990, 5, 15)));
        when(rs.getString("ID_NUMBER")).thenReturn("ID12345678");
        when(rs.getString("ADDRESS_LINE_1")).thenReturn("123 Main Street");
        when(rs.getString("ADDRESS_LINE_2")).thenReturn("Flat 4");
        when(rs.getString("CITY")).thenReturn("Johannesburg");
        when(rs.getString("STATE_PROVINCE")).thenReturn("Gauteng");
        when(rs.getString("POSTAL_CODE")).thenReturn("2000");
        when(rs.getString("COUNTRY")).thenReturn("ZA");
        when(rs.getString("PHONE_NUMBER")).thenReturn("0123456789");
        when(rs.getString("EMAIL_ADDRESS")).thenReturn("jane@example.com");
        when(rs.getDate("CREATED_DATE")).thenReturn(Date.valueOf(LocalDate.of(2020, 1, 2)));
        when(rs.getString("RECORD_STATUS")).thenReturn("A");
    }

    private Candidate candidate() {
        Candidate c = new Candidate();
        c.setFirstName("Jane");
        c.setLastName("Smith");
        c.setDateOfBirth(LocalDate.of(1990, 5, 15));
        c.setIdNumber("ID12345678");
        c.setAddressLine1("123 Main Street");
        c.setAddressLine2("Flat 4");
        c.setCity("Johannesburg");
        c.setStateProvince("Gauteng");
        c.setPostalCode("2000");
        c.setCountry("ZA");
        c.setPhoneNumber("0123456789");
        c.setEmailAddress("jane@example.com");
        return c;
    }
}
