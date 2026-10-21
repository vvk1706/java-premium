package com.dlis.licensing.service.ejb;

import com.dlis.licensing.dao.ApplicationDao;
import com.dlis.licensing.client.CandidateGateway;
import com.dlis.licensing.dao.DrivingHistoryDao;
import com.dlis.licensing.dao.IssuedLicenseDao;
import com.dlis.licensing.domain.*;
import com.dlis.common.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApplicationServiceEjb — eligibility and history check logic.
 * Business rules verified against AB-CHECK-ELIGIBILITY and AB-CHECK-HISTORY.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceEjbTest {

    @Mock private ApplicationDao    applicationDao;
    @Mock private CandidateGateway  candidateGateway;
    @Mock private DrivingHistoryDao historyDao;
    @Mock private IssuedLicenseDao  licenseDao;

    @InjectMocks
    private ApplicationServiceEjb service;

    private CandidateRef candidate16;
    private LicenseApplication learnerApp;

    @BeforeEach
    void setUp() {
        candidate16 = new CandidateRef(1L, LocalDate.now().minusYears(16), true);

        learnerApp = new LicenseApplication();
        learnerApp.setApplicationId(100L);
        learnerApp.setCandidateId(1L);
        learnerApp.setLicenseType("L");
        learnerApp.setApplicationStatus(LicenseApplication.STATUS_PENDING);
        learnerApp.setEligibilityChkStatus("U");
        learnerApp.setHistoryChkStatus("U");
        learnerApp.setPaymentStatus("U");
    }

    // ── createApplication ────────────────────────────────────────────────────

    @Test
    void createApplication_success() {
        when(candidateGateway.findById(1L)).thenReturn(Optional.of(candidate16));
        when(applicationDao.findActiveByCandidate(1L, "L"))
                .thenReturn(Collections.emptyList());
        doAnswer(inv -> {
            LicenseApplication a = inv.getArgument(0);
            a.setApplicationId(100L);
            return null;
        }).when(applicationDao).insert(any());

        ServiceResult<Long> result = service.createApplication(1L, "L", "OFFICER1");

        assertTrue(result.isOk());
        assertEquals(100L, result.getPayload());
    }

    @Test
    void createApplication_candidateNotFound() {
        when(candidateGateway.findById(99L)).thenReturn(Optional.empty());

        ServiceResult<Long> result = service.createApplication(99L, "L", "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    @Test
    void createApplication_invalidLicenseType() {
        when(candidateGateway.findById(1L)).thenReturn(Optional.of(candidate16));

        ServiceResult<Long> result = service.createApplication(1L, "X", "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_TYPE, result.getReturnCode());
    }

    // ── checkEligibility ─────────────────────────────────────────────────────

    @Test
    void checkEligibility_learner_16yo_passes() {
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));
        when(candidateGateway.findById(1L)).thenReturn(Optional.of(candidate16));

        ServiceResult<String> result = service.checkEligibility(100L, "OFFICER1");

        assertTrue(result.isOk());
        assertEquals("P", result.getPayload());
        verify(applicationDao).updateEligibilityCheck(
                eq(100L), eq("P"), any(), anyString(),
                eq(LicenseApplication.STATUS_ELIG_CHECKED), any(), anyString());
    }

    @Test
    void checkEligibility_learner_underage_fails() {
        candidate16 = new CandidateRef(1L, LocalDate.now().minusYears(15), true); // only 15
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));
        when(candidateGateway.findById(1L)).thenReturn(Optional.of(candidate16));

        ServiceResult<String> result = service.checkEligibility(100L, "OFFICER1");

        assertTrue(result.isOk()); // service returns ok with F status
        assertEquals("F", result.getPayload());
        verify(applicationDao).updateEligibilityCheck(
                eq(100L), eq("F"), any(), anyString(),
                eq(LicenseApplication.STATUS_REJECTED), any(), anyString());
    }

    @Test
    void checkEligibility_probation_requiresActiveLearner() {
        LicenseApplication probApp = new LicenseApplication();
        probApp.setApplicationId(101L);
        probApp.setCandidateId(1L);
        probApp.setLicenseType("P");
        probApp.setEligibilityChkStatus("U");

        CandidateRef c17 = new CandidateRef(1L, LocalDate.now().minusYears(17), true);

        when(applicationDao.findById(101L)).thenReturn(Optional.of(probApp));
        when(candidateGateway.findById(1L)).thenReturn(Optional.of(c17));
        // No active Learner license
        when(licenseDao.findActiveByCandidate(1L, "L"))
                .thenReturn(Collections.emptyList());

        ServiceResult<String> result = service.checkEligibility(101L, "OFFICER1");

        assertEquals("F", result.getPayload());
        assertTrue(result.getMessage().contains("LEARNER"));
    }

    // ── checkHistory ──────────────────────────────────────────────────────────

    @Test
    void checkHistory_noRecords_passes() {
        learnerApp.setEligibilityChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));
        when(historyDao.findActiveByCandidateId(1L))
                .thenReturn(Collections.emptyList());

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertTrue(result.isOk());
        assertEquals("P", result.getPayload());
    }

    @Test
    void checkHistory_unpaidFine_fails() {
        learnerApp.setEligibilityChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        DrivingHistory badHistory = new DrivingHistory();
        badHistory.setIncidentType("OF");
        badHistory.setDemeritPoints(2);
        badHistory.setFineAmount(new BigDecimal("500.00"));
        badHistory.setFinePaidStatus("N");
        badHistory.setRecordStatus("A");

        when(historyDao.findActiveByCandidateId(1L))
                .thenReturn(List.of(badHistory));

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertEquals("F", result.getPayload());
        assertTrue(result.getMessage().contains("UNPAID FINES"));
    }

    @Test
    void checkHistory_demeritExceeds12_fails() {
        learnerApp.setEligibilityChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        DrivingHistory h = new DrivingHistory();
        h.setIncidentType("OF");
        h.setDemeritPoints(13);
        h.setFineAmount(BigDecimal.ZERO);
        h.setRecordStatus("A");

        when(historyDao.findActiveByCandidateId(1L)).thenReturn(List.of(h));

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertEquals("F", result.getPayload());
        assertTrue(result.getMessage().contains("DEMERIT"));
    }

    @Test
    void checkHistory_eligibilityNotPassed_returnsError() {
        learnerApp.setEligibilityChkStatus("U");  // not passed
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        ServiceResult<String> result = service.checkHistory(100L, "OFFICER1");

        assertFalse(result.isOk());
        assertTrue(result.getMessage().contains("ELIGIBILITY"));
    }

    // ── inquireStatus ─────────────────────────────────────────────────────────

    @Test
    void inquireStatus_found_returnsApplication() {
        // Arrange
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        // Act
        ServiceResult<LicenseApplication> result = service.inquireStatus(100L);

        // Assert
        assertTrue(result.isOk());
        assertEquals(100L, result.getPayload().getApplicationId());
    }

    @Test
    void inquireStatus_notFound_returnsError() {
        // Arrange
        when(applicationDao.findById(999L)).thenReturn(Optional.empty());

        // Act
        ServiceResult<LicenseApplication> result = service.inquireStatus(999L);

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        assertTrue(result.getMessage().contains("APPLICATION NOT FOUND"));
    }

    // ── candidate lookups via CandidateGateway ───────────────────────────────

    @Test
    void createApplication_inactiveCandidate_rejected() {
        when(candidateGateway.findById(1L))
                .thenReturn(Optional.of(new CandidateRef(1L, LocalDate.now().minusYears(30), false)));

        ServiceResult<Long> result = service.createApplication(1L, "L", "OFFICER1");

        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        verify(applicationDao, never()).insert(any());
    }

    @Test
    void createApplication_unknownCandidate_rejected() {
        when(candidateGateway.findById(9L)).thenReturn(Optional.empty());

        assertEquals(ServiceResult.RC_NOT_FOUND, service.createApplication(9L, "L", "X").getReturnCode());
    }

    @Test
    void createApplication_candidateServiceDown_propagatesAndWritesNothing() {
        when(candidateGateway.findById(1L))
                .thenThrow(new com.dlis.common.RemoteServiceException("candidate down"));

        assertThrows(com.dlis.common.RemoteServiceException.class,
                () -> service.createApplication(1L, "L", "X"));
        verify(applicationDao, never()).insert(any());
    }

    // ── recordPayment (saga callback from dlis-payment-service) ──────────────

    @Test
    void recordPayment_historyPassed_marksPaid() {
        learnerApp.setHistoryChkStatus("P");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        ServiceResult<Void> result = service.recordPayment(100L, "REF-1", "OFFICER1");

        assertTrue(result.isOk());
        verify(applicationDao).updatePayment(eq(100L), eq("P"), eq("REF-1"),
                eq(LicenseApplication.STATUS_PAYMENT_APPROVED), any(), eq("OFFICER1"));
    }

    @Test
    void recordPayment_unknownApplication_notFound() {
        when(applicationDao.findById(5L)).thenReturn(Optional.empty());

        assertEquals(ServiceResult.RC_NOT_FOUND, service.recordPayment(5L, "R", "U").getReturnCode());
    }

    @Test
    void recordPayment_historyNotPassed_rejected() {
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        ServiceResult<Void> result = service.recordPayment(100L, "R", "U");

        assertEquals(ServiceResult.RC_HISTORY_NOT_PASSED, result.getReturnCode());
        verify(applicationDao, never()).updatePayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordPayment_sameReferenceTwice_isIdempotent() {
        learnerApp.setHistoryChkStatus("P");
        learnerApp.setPaymentStatus("P");
        learnerApp.setPaymentReference("REF-1");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        ServiceResult<Void> result = service.recordPayment(100L, "REF-1", "U");

        assertTrue(result.isOk());
        verify(applicationDao, never()).updatePayment(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordPayment_differentReferenceWhenAlreadyPaid_rejected() {
        learnerApp.setHistoryChkStatus("P");
        learnerApp.setPaymentStatus("P");
        learnerApp.setPaymentReference("REF-1");
        when(applicationDao.findById(100L)).thenReturn(Optional.of(learnerApp));

        assertEquals(ServiceResult.RC_ALREADY_PAID,
                service.recordPayment(100L, "REF-2", "U").getReturnCode());
    }
}
