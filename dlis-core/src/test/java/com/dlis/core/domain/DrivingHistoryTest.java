package com.dlis.core.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DrivingHistoryTest {

    private DrivingHistory history;

    @BeforeEach
    void setUp() {
        history = new DrivingHistory();
        history.setHistoryId(100L);
        history.setCandidateId(10L);
        history.setIncidentDate(LocalDate.of(2023, 6, 1));
        history.setDemeritPoints(3);
        history.setRecordStatus("A");
    }

    // ── isActiveSuspensionOrDisqualification ──────────────────────────────────

    @Test
    void isActiveSuspensionOrDisqualification_suspensionNoEndDate_returnsTrue() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_SUSPENSION);
        history.setSuspensionEndDate(null);

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_suspensionFutureEndDate_returnsTrue() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_SUSPENSION);
        history.setSuspensionEndDate(LocalDate.now().plusDays(30));

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_suspensionEndDateToday_returnsTrue() {
        // Arrange — end date = today (not yet past)
        history.setIncidentType(DrivingHistory.TYPE_SUSPENSION);
        history.setSuspensionEndDate(LocalDate.now());

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_suspensionPastEndDate_returnsFalse() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_SUSPENSION);
        history.setSuspensionEndDate(LocalDate.now().minusDays(1));

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_disqualificationNoEndDate_returnsTrue() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_DISQUALIFICATION);
        history.setSuspensionEndDate(null);

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_offenceType_returnsFalse() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_OFFENCE);
        history.setSuspensionEndDate(null);

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_accidentType_returnsFalse() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_ACCIDENT);
        history.setSuspensionEndDate(null);

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActiveSuspensionOrDisqualification_nullIncidentType_returnsFalse() {
        // Arrange
        history.setIncidentType(null);
        history.setSuspensionEndDate(null);

        // Act
        boolean result = history.isActiveSuspensionOrDisqualification();

        // Assert
        assertFalse(result);
    }

    // ── hasUnpaidFine ─────────────────────────────────────────────────────────

    @Test
    void hasUnpaidFine_positiveFineAndNotPaid_returnsTrue() {
        // Arrange
        history.setFineAmount(new BigDecimal("500.00"));
        history.setFinePaidStatus("N");

        // Act
        boolean result = history.hasUnpaidFine();

        // Assert
        assertTrue(result);
    }

    @Test
    void hasUnpaidFine_positiveFineAndPaid_returnsFalse() {
        // Arrange
        history.setFineAmount(new BigDecimal("500.00"));
        history.setFinePaidStatus("Y");

        // Act
        boolean result = history.hasUnpaidFine();

        // Assert
        assertFalse(result);
    }

    @Test
    void hasUnpaidFine_zeroFineAndNotPaid_returnsFalse() {
        // Arrange
        history.setFineAmount(BigDecimal.ZERO);
        history.setFinePaidStatus("N");

        // Act
        boolean result = history.hasUnpaidFine();

        // Assert
        assertFalse(result);
    }

    @Test
    void hasUnpaidFine_nullFineAmount_returnsFalse() {
        // Arrange
        history.setFineAmount(null);
        history.setFinePaidStatus("N");

        // Act
        boolean result = history.hasUnpaidFine();

        // Assert
        assertFalse(result);
    }

    @Test
    void hasUnpaidFine_nullFinePaidStatus_returnsFalse() {
        // Arrange
        history.setFineAmount(new BigDecimal("200.00"));
        history.setFinePaidStatus(null);

        // Act
        boolean result = history.hasUnpaidFine();

        // Assert
        assertFalse(result);
    }

    // ── Type constants ────────────────────────────────────────────────────────

    @Test
    void typeConstants_haveExpectedValues() {
        assertEquals("OF", DrivingHistory.TYPE_OFFENCE);
        assertEquals("AC", DrivingHistory.TYPE_ACCIDENT);
        assertEquals("SU", DrivingHistory.TYPE_SUSPENSION);
        assertEquals("DQ", DrivingHistory.TYPE_DISQUALIFICATION);
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Arrange
        history.setIncidentType(DrivingHistory.TYPE_OFFENCE);
        history.setIncidentDescription("Speeding");
        history.setFineAmount(new BigDecimal("300.00"));
        history.setFinePaidStatus("Y");
        history.setSuspensionStartDate(LocalDate.of(2023, 7, 1));
        history.setSuspensionEndDate(LocalDate.of(2023, 8, 1));
        history.setCourtCaseNumber("CC-2023-001");
        history.setRecordedByAuthority("TRAFFIC-01");

        // Assert
        assertEquals(100L, history.getHistoryId());
        assertEquals(10L, history.getCandidateId());
        assertEquals(LocalDate.of(2023, 6, 1), history.getIncidentDate());
        assertEquals(DrivingHistory.TYPE_OFFENCE, history.getIncidentType());
        assertEquals("Speeding", history.getIncidentDescription());
        assertEquals(3, history.getDemeritPoints());
        assertEquals(new BigDecimal("300.00"), history.getFineAmount());
        assertEquals("Y", history.getFinePaidStatus());
        assertEquals(LocalDate.of(2023, 7, 1), history.getSuspensionStartDate());
        assertEquals(LocalDate.of(2023, 8, 1), history.getSuspensionEndDate());
        assertEquals("CC-2023-001", history.getCourtCaseNumber());
        assertEquals("TRAFFIC-01", history.getRecordedByAuthority());
        assertEquals("A", history.getRecordStatus());
    }
}
