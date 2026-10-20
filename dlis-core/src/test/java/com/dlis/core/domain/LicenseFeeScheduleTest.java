package com.dlis.core.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LicenseFeeScheduleTest {

    private LicenseFeeSchedule schedule;

    @BeforeEach
    void setUp() {
        schedule = new LicenseFeeSchedule();
        schedule.setFeeScheduleId(1L);
        schedule.setLicenseType("L");
        schedule.setFeeType("IF");
        schedule.setFeeAmount(new BigDecimal("150.00"));
        schedule.setEffectiveDate(LocalDate.now().minusDays(30));
        schedule.setExpiryDate(LocalDate.now().plusDays(30));
        schedule.setCurrencyCode("ZAR");
        schedule.setDescription("Initial fee for Learner");
        schedule.setActiveStatus("A");
    }

    // ── isActive ──────────────────────────────────────────────────────────────

    @Test
    void isActive_activeStatusEffectiveTodayNoExpiry_returnsTrue() {
        // Arrange
        schedule.setActiveStatus("A");
        schedule.setEffectiveDate(LocalDate.now());
        schedule.setExpiryDate(null);

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_activeStatusEffectiveInPastExpiryInFuture_returnsTrue() {
        // Arrange
        schedule.setActiveStatus("A");
        schedule.setEffectiveDate(LocalDate.now().minusDays(10));
        schedule.setExpiryDate(LocalDate.now().plusDays(10));

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_activeStatusEffectiveInPastNullExpiry_returnsTrue() {
        // Arrange
        schedule.setActiveStatus("A");
        schedule.setEffectiveDate(LocalDate.now().minusDays(1));
        schedule.setExpiryDate(null);

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_activeStatusExpiryEqualsToday_returnsTrue() {
        // Arrange
        schedule.setActiveStatus("A");
        schedule.setEffectiveDate(LocalDate.now().minusDays(5));
        schedule.setExpiryDate(LocalDate.now());

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_inactiveStatus_returnsFalse() {
        // Arrange
        schedule.setActiveStatus("I");
        schedule.setEffectiveDate(LocalDate.now().minusDays(10));
        schedule.setExpiryDate(LocalDate.now().plusDays(10));

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActive_effectiveDateInFuture_returnsFalse() {
        // Arrange
        schedule.setActiveStatus("A");
        schedule.setEffectiveDate(LocalDate.now().plusDays(1));
        schedule.setExpiryDate(LocalDate.now().plusDays(30));

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActive_expiryDateInPast_returnsFalse() {
        // Arrange
        schedule.setActiveStatus("A");
        schedule.setEffectiveDate(LocalDate.now().minusDays(30));
        schedule.setExpiryDate(LocalDate.now().minusDays(1));

        // Act
        boolean result = schedule.isActive();

        // Assert
        assertFalse(result);
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Arrange
        LocalDate eff = LocalDate.of(2024, 1, 1);
        LocalDate exp = LocalDate.of(2025, 12, 31);
        schedule.setFeeScheduleId(99L);
        schedule.setLicenseType("P");
        schedule.setFeeType("RF");
        schedule.setFeeAmount(new BigDecimal("200.50"));
        schedule.setEffectiveDate(eff);
        schedule.setExpiryDate(exp);
        schedule.setCurrencyCode("USD");
        schedule.setDescription("Renewal fee for Probationary");
        schedule.setActiveStatus("I");

        // Assert
        assertEquals(99L, schedule.getFeeScheduleId());
        assertEquals("P", schedule.getLicenseType());
        assertEquals("RF", schedule.getFeeType());
        assertEquals(new BigDecimal("200.50"), schedule.getFeeAmount());
        assertEquals(eff, schedule.getEffectiveDate());
        assertEquals(exp, schedule.getExpiryDate());
        assertEquals("USD", schedule.getCurrencyCode());
        assertEquals("Renewal fee for Probationary", schedule.getDescription());
        assertEquals("I", schedule.getActiveStatus());
    }
}
