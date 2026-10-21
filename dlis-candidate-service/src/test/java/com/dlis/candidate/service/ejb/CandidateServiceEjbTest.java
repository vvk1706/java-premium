package com.dlis.candidate.service.ejb;

import com.dlis.candidate.dao.CandidateDao;
import com.dlis.candidate.domain.Candidate;
import com.dlis.common.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CandidateServiceEjb}.
 * Derived from the business rules in AB-CREATE-CANDIDATE (BR-CAND-001, BR-CAND-002, BR-CAND-003).
 */
@ExtendWith(MockitoExtension.class)
class CandidateServiceEjbTest {

    @Mock
    private CandidateDao candidateDao;

    @InjectMocks
    private CandidateServiceEjb service;

    private Candidate validCandidate;

    @BeforeEach
    void setUp() {
        validCandidate = new Candidate();
        validCandidate.setFirstName("Jane");
        validCandidate.setLastName("Smith");
        validCandidate.setDateOfBirth(LocalDate.of(1990, 5, 15));
        validCandidate.setIdNumber("ID12345678");
        validCandidate.setAddressLine1("123 Main Street");
        validCandidate.setCity("Johannesburg");
        validCandidate.setStateProvince("Gauteng");
        validCandidate.setPostalCode("2000");
        validCandidate.setCountry("ZA");
    }

    // ── createCandidate ───────────────────────────────────────────────────────

    @Test
    void createCandidate_success() {
        // Arrange
        when(candidateDao.findByIdNumber("ID12345678")).thenReturn(Optional.empty());
        doAnswer(inv -> {
            Candidate c = inv.getArgument(0);
            c.setCandidateId(1001L);
            return null;
        }).when(candidateDao).insert(any(Candidate.class));

        // Act
        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        // Assert
        assertTrue(result.isOk());
        assertEquals(1001L, result.getPayload());
        assertEquals("CANDIDATE CREATED SUCCESSFULLY", result.getMessage());
        verify(candidateDao).insert(validCandidate);
    }

    @Test
    void createCandidate_success_setsDefaultFields() {
        // Arrange
        when(candidateDao.findByIdNumber("ID12345678")).thenReturn(Optional.empty());
        doAnswer(inv -> {
            Candidate c = inv.getArgument(0);
            c.setCandidateId(1001L);
            return null;
        }).when(candidateDao).insert(any(Candidate.class));

        // Act
        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        // Assert
        assertTrue(result.isOk());
        ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateDao).insert(captor.capture());
        Candidate persisted = captor.getValue();
        assertEquals("A", persisted.getRecordStatus());
        assertEquals(LocalDate.now(), persisted.getCreatedDate());
    }

    @Test
    void createCandidate_duplicateIdNumber_returnsError() {
        // Arrange
        when(candidateDao.findByIdNumber("ID12345678"))
                .thenReturn(Optional.of(validCandidate));

        // Act
        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_DUPLICATE, result.getReturnCode());
        assertTrue(result.getMessage().contains("ALREADY EXISTS"));
        verify(candidateDao, never()).insert(any());
    }

    @Test
    void createCandidate_futureDateOfBirth_returnsError() {
        // Arrange
        when(candidateDao.findByIdNumber(any())).thenReturn(Optional.empty());
        validCandidate.setDateOfBirth(LocalDate.now().plusDays(1));

        // Act
        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_AGE, result.getReturnCode());
        assertEquals("DATE OF BIRTH MUST BE IN THE PAST", result.getMessage());
        verify(candidateDao, never()).insert(any());
    }

    @Test
    void createCandidate_todayDateOfBirth_returnsError() {
        // Arrange
        when(candidateDao.findByIdNumber(any())).thenReturn(Optional.empty());
        validCandidate.setDateOfBirth(LocalDate.now());

        // Act
        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_AGE, result.getReturnCode());
        assertEquals("DATE OF BIRTH MUST BE IN THE PAST", result.getMessage());
        verify(candidateDao, never()).insert(any());
    }

    @Test
    void createCandidate_nullDateOfBirth_returnsError() {
        // Arrange
        when(candidateDao.findByIdNumber(any())).thenReturn(Optional.empty());
        validCandidate.setDateOfBirth(null);

        // Act
        ServiceResult<Long> result = service.createCandidate(validCandidate, "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_AGE, result.getReturnCode());
        assertEquals("DATE OF BIRTH MUST BE IN THE PAST", result.getMessage());
        verify(candidateDao, never()).insert(any());
    }

    // ── findByIdNumber ────────────────────────────────────────────────────────

    @Test
    void findByIdNumber_found() {
        // Arrange
        when(candidateDao.findByIdNumber("ID12345678")).thenReturn(Optional.of(validCandidate));

        // Act
        ServiceResult<Candidate> result = service.findByIdNumber("ID12345678");

        // Assert
        assertTrue(result.isOk());
        assertEquals("Jane", result.getPayload().getFirstName());
        assertEquals("Smith", result.getPayload().getLastName());
        assertEquals("OK", result.getMessage());
    }

    @Test
    void findByIdNumber_notFound() {
        // Arrange
        when(candidateDao.findByIdNumber("NONEXISTENT")).thenReturn(Optional.empty());

        // Act
        ServiceResult<Candidate> result = service.findByIdNumber("NONEXISTENT");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        assertEquals("CANDIDATE NOT FOUND", result.getMessage());
        assertNull(result.getPayload());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_found() {
        // Arrange
        validCandidate.setCandidateId(42L);
        when(candidateDao.findById(42L)).thenReturn(Optional.of(validCandidate));

        // Act
        ServiceResult<Candidate> result = service.findById(42L);

        // Assert
        assertTrue(result.isOk());
        assertEquals("Jane", result.getPayload().getFirstName());
        assertEquals("OK", result.getMessage());
    }

    @Test
    void findById_notFound() {
        // Arrange
        when(candidateDao.findById(99L)).thenReturn(Optional.empty());

        // Act
        ServiceResult<Candidate> result = service.findById(99L);

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        assertEquals("CANDIDATE NOT FOUND", result.getMessage());
        assertNull(result.getPayload());
    }
}
