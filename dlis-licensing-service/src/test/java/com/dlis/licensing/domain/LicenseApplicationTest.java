package com.dlis.licensing.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LicenseApplicationTest {

    private LicenseApplication app;

    @BeforeEach
    void setUp() {
        app = new LicenseApplication();
        app.setApplicationId(300L);
        app.setCandidateId(10L);
        app.setLicenseType("L");
        app.setApplicationDate(LocalDate.of(2024, 5, 1));
        app.setCreatedDate(LocalDate.of(2024, 5, 1));
        app.setCreatedBy("OFFICER1");
    }

    // ── isEligibilityPassed ───────────────────────────────────────────────────

    @Test
    void isEligibilityPassed_passStatus_returnsTrue() {
        // Arrange
        app.setEligibilityChkStatus(LicenseApplication.CHECK_PASS);

        // Act & Assert
        assertTrue(app.isEligibilityPassed());
    }

    @Test
    void isEligibilityPassed_failStatus_returnsFalse() {
        // Arrange
        app.setEligibilityChkStatus(LicenseApplication.CHECK_FAIL);

        // Act & Assert
        assertFalse(app.isEligibilityPassed());
    }

    @Test
    void isEligibilityPassed_uncheckedStatus_returnsFalse() {
        // Arrange
        app.setEligibilityChkStatus(LicenseApplication.CHECK_UNCHECKED);

        // Act & Assert
        assertFalse(app.isEligibilityPassed());
    }

    @Test
    void isEligibilityPassed_nullStatus_returnsFalse() {
        // Arrange
        app.setEligibilityChkStatus(null);

        // Act & Assert
        assertFalse(app.isEligibilityPassed());
    }

    // ── isHistoryPassed ───────────────────────────────────────────────────────

    @Test
    void isHistoryPassed_passStatus_returnsTrue() {
        // Arrange
        app.setHistoryChkStatus(LicenseApplication.CHECK_PASS);

        // Act & Assert
        assertTrue(app.isHistoryPassed());
    }

    @Test
    void isHistoryPassed_failStatus_returnsFalse() {
        // Arrange
        app.setHistoryChkStatus(LicenseApplication.CHECK_FAIL);

        // Act & Assert
        assertFalse(app.isHistoryPassed());
    }

    @Test
    void isHistoryPassed_nullStatus_returnsFalse() {
        // Arrange
        app.setHistoryChkStatus(null);

        // Act & Assert
        assertFalse(app.isHistoryPassed());
    }

    // ── isPaymentComplete ─────────────────────────────────────────────────────

    @Test
    void isPaymentComplete_paidStatus_returnsTrue() {
        // Arrange
        app.setPaymentStatus(LicenseApplication.PAYMENT_PAID);

        // Act & Assert
        assertTrue(app.isPaymentComplete());
    }

    @Test
    void isPaymentComplete_waivedStatus_returnsTrue() {
        // Arrange
        app.setPaymentStatus(LicenseApplication.PAYMENT_WAIVED);

        // Act & Assert
        assertTrue(app.isPaymentComplete());
    }

    @Test
    void isPaymentComplete_unpaidStatus_returnsFalse() {
        // Arrange
        app.setPaymentStatus(LicenseApplication.PAYMENT_UNPAID);

        // Act & Assert
        assertFalse(app.isPaymentComplete());
    }

    @Test
    void isPaymentComplete_nullStatus_returnsFalse() {
        // Arrange
        app.setPaymentStatus(null);

        // Act & Assert
        assertFalse(app.isPaymentComplete());
    }

    // ── isApproval1Complete / isApproval2Complete ─────────────────────────────

    @Test
    void isApproval1Complete_approvedDecision_returnsTrue() {
        // Arrange
        app.setApproval1Status(LicenseApplication.DECISION_APPROVED);

        // Act & Assert
        assertTrue(app.isApproval1Complete());
    }

    @Test
    void isApproval1Complete_rejectedDecision_returnsFalse() {
        // Arrange
        app.setApproval1Status(LicenseApplication.DECISION_REJECTED);

        // Act & Assert
        assertFalse(app.isApproval1Complete());
    }

    @Test
    void isApproval1Complete_nullDecision_returnsFalse() {
        // Arrange
        app.setApproval1Status(null);

        // Act & Assert
        assertFalse(app.isApproval1Complete());
    }

    @Test
    void isApproval2Complete_approvedDecision_returnsTrue() {
        // Arrange
        app.setApproval2Status(LicenseApplication.DECISION_APPROVED);

        // Act & Assert
        assertTrue(app.isApproval2Complete());
    }

    @Test
    void isApproval2Complete_rejectedDecision_returnsFalse() {
        // Arrange
        app.setApproval2Status(LicenseApplication.DECISION_REJECTED);

        // Act & Assert
        assertFalse(app.isApproval2Complete());
    }

    // ── isFullyApproved / isIssued / isRejected ───────────────────────────────

    @Test
    void isFullyApproved_approvedStatus_returnsTrue() {
        // Arrange
        app.setApplicationStatus(LicenseApplication.STATUS_APPROVED);

        // Act & Assert
        assertTrue(app.isFullyApproved());
    }

    @Test
    void isFullyApproved_pendingStatus_returnsFalse() {
        // Arrange
        app.setApplicationStatus(LicenseApplication.STATUS_PENDING);

        // Act & Assert
        assertFalse(app.isFullyApproved());
    }

    @Test
    void isIssued_issuedStatus_returnsTrue() {
        // Arrange
        app.setApplicationStatus(LicenseApplication.STATUS_ISSUED);

        // Act & Assert
        assertTrue(app.isIssued());
    }

    @Test
    void isIssued_approvedStatus_returnsFalse() {
        // Arrange
        app.setApplicationStatus(LicenseApplication.STATUS_APPROVED);

        // Act & Assert
        assertFalse(app.isIssued());
    }

    @Test
    void isRejected_rejectedStatus_returnsTrue() {
        // Arrange
        app.setApplicationStatus(LicenseApplication.STATUS_REJECTED);

        // Act & Assert
        assertTrue(app.isRejected());
    }

    @Test
    void isRejected_issuedStatus_returnsFalse() {
        // Arrange
        app.setApplicationStatus(LicenseApplication.STATUS_ISSUED);

        // Act & Assert
        assertFalse(app.isRejected());
    }

    // ── Status / check constants ──────────────────────────────────────────────

    @Test
    void statusConstants_haveExpectedValues() {
        assertEquals("PE", LicenseApplication.STATUS_PENDING);
        assertEquals("EC", LicenseApplication.STATUS_ELIG_CHECKED);
        assertEquals("HC", LicenseApplication.STATUS_HIST_CHECKED);
        assertEquals("PP", LicenseApplication.STATUS_PAYMENT_PENDING);
        assertEquals("PA", LicenseApplication.STATUS_PAYMENT_APPROVED);
        assertEquals("A1", LicenseApplication.STATUS_APPROVAL1_PEND);
        assertEquals("A2", LicenseApplication.STATUS_APPROVAL2_PEND);
        assertEquals("AP", LicenseApplication.STATUS_APPROVED);
        assertEquals("RE", LicenseApplication.STATUS_REJECTED);
        assertEquals("IS", LicenseApplication.STATUS_ISSUED);
    }

    @Test
    void checkConstants_haveExpectedValues() {
        assertEquals("P", LicenseApplication.CHECK_PASS);
        assertEquals("F", LicenseApplication.CHECK_FAIL);
        assertEquals("U", LicenseApplication.CHECK_UNCHECKED);
    }

    @Test
    void paymentConstants_haveExpectedValues() {
        assertEquals("P", LicenseApplication.PAYMENT_PAID);
        assertEquals("U", LicenseApplication.PAYMENT_UNPAID);
        assertEquals("W", LicenseApplication.PAYMENT_WAIVED);
    }

    @Test
    void decisionConstants_haveExpectedValues() {
        assertEquals("A", LicenseApplication.DECISION_APPROVED);
        assertEquals("R", LicenseApplication.DECISION_REJECTED);
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Arrange — additional fields
        app.setApplicationStatus(LicenseApplication.STATUS_PENDING);
        app.setEligibilityChkDate(LocalDate.of(2024, 5, 2));
        app.setEligibilityChkNotes("All docs OK");
        app.setHistoryChkDate(LocalDate.of(2024, 5, 3));
        app.setHistoryChkNotes("Clean record");
        app.setPaymentReference("RCP20240501300");
        app.setApproval1Authority("AUTH-01");
        app.setApproval1Date(LocalDate.of(2024, 5, 10));
        app.setApproval1Notes("Approved by L1");
        app.setApproval2Authority("AUTH-02");
        app.setApproval2Date(LocalDate.of(2024, 5, 11));
        app.setApproval2Notes("Approved by L2");
        app.setRejectionReason(null);
        app.setLastUpdatedDate(LocalDate.of(2024, 5, 11));
        app.setLastUpdatedBy("OFFICER2");

        // Assert
        assertEquals(300L, app.getApplicationId());
        assertEquals(10L, app.getCandidateId());
        assertEquals("L", app.getLicenseType());
        assertEquals(LocalDate.of(2024, 5, 1), app.getApplicationDate());
        assertEquals(LicenseApplication.STATUS_PENDING, app.getApplicationStatus());
        assertEquals(LocalDate.of(2024, 5, 2), app.getEligibilityChkDate());
        assertEquals("All docs OK", app.getEligibilityChkNotes());
        assertEquals(LocalDate.of(2024, 5, 3), app.getHistoryChkDate());
        assertEquals("Clean record", app.getHistoryChkNotes());
        assertEquals("RCP20240501300", app.getPaymentReference());
        assertEquals("AUTH-01", app.getApproval1Authority());
        assertEquals(LocalDate.of(2024, 5, 10), app.getApproval1Date());
        assertEquals("Approved by L1", app.getApproval1Notes());
        assertEquals("AUTH-02", app.getApproval2Authority());
        assertEquals(LocalDate.of(2024, 5, 11), app.getApproval2Date());
        assertEquals("Approved by L2", app.getApproval2Notes());
        assertNull(app.getRejectionReason());
        assertEquals(LocalDate.of(2024, 5, 1), app.getCreatedDate());
        assertEquals("OFFICER1", app.getCreatedBy());
        assertEquals(LocalDate.of(2024, 5, 11), app.getLastUpdatedDate());
        assertEquals("OFFICER2", app.getLastUpdatedBy());
    }
}
