package com.dlis.core.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IssuedLicenseTest {

    private IssuedLicense license;

    @BeforeEach
    void setUp() {
        license = new IssuedLicense();
        license.setLicenseId(200L);
        license.setApplicationId(50L);
        license.setCandidateId(10L);
        license.setLicenseNumber("LIC-2024-200");
        license.setLicenseType("L");
        license.setIssueDate(LocalDate.of(2024, 3, 1));
        license.setExpiryDate(LocalDate.of(2026, 3, 1));
        license.setVehicleClass("B");
        license.setDemeritBalance(0);
        license.setIssuedByAuthority("DLIS-HQ");
        license.setIssuedByOfficer("OFC-01");
        license.setRenewalCount(0);
    }

    // ── isActive ──────────────────────────────────────────────────────────────

    @Test
    void isActive_activeStatus_returnsTrue() {
        // Arrange
        license.setLicenseStatus(IssuedLicense.STATUS_ACTIVE);

        // Act
        boolean result = license.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_suspendedStatus_returnsFalse() {
        // Arrange
        license.setLicenseStatus(IssuedLicense.STATUS_SUSPENDED);

        // Act
        boolean result = license.isActive();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActive_nullStatus_returnsFalse() {
        // Arrange
        license.setLicenseStatus(null);

        // Act
        boolean result = license.isActive();

        // Assert
        assertFalse(result);
    }

    // ── isExpired ─────────────────────────────────────────────────────────────

    @Test
    void isExpired_expiredStatus_returnsTrue() {
        // Arrange
        license.setLicenseStatus(IssuedLicense.STATUS_EXPIRED);

        // Act
        boolean result = license.isExpired();

        // Assert
        assertTrue(result);
    }

    @Test
    void isExpired_activeStatus_returnsFalse() {
        // Arrange
        license.setLicenseStatus(IssuedLicense.STATUS_ACTIVE);

        // Act
        boolean result = license.isExpired();

        // Assert
        assertFalse(result);
    }

    @Test
    void isExpired_nullStatus_returnsFalse() {
        // Arrange
        license.setLicenseStatus(null);

        // Act
        boolean result = license.isExpired();

        // Assert
        assertFalse(result);
    }

    // ── Status constants ──────────────────────────────────────────────────────

    @Test
    void statusConstants_haveExpectedValues() {
        assertEquals("A", IssuedLicense.STATUS_ACTIVE);
        assertEquals("S", IssuedLicense.STATUS_SUSPENDED);
        assertEquals("E", IssuedLicense.STATUS_EXPIRED);
        assertEquals("C", IssuedLicense.STATUS_CANCELLED);
        assertEquals("R", IssuedLicense.STATUS_REVOKED);
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Arrange — additional fields
        license.setLicenseStatus(IssuedLicense.STATUS_ACTIVE);
        license.setRestrictions("None");
        license.setPreviousLicenseId(100L);
        license.setNotes("Issued after road test");

        // Assert
        assertEquals(200L, license.getLicenseId());
        assertEquals(50L, license.getApplicationId());
        assertEquals(10L, license.getCandidateId());
        assertEquals("LIC-2024-200", license.getLicenseNumber());
        assertEquals("L", license.getLicenseType());
        assertEquals(LocalDate.of(2024, 3, 1), license.getIssueDate());
        assertEquals(LocalDate.of(2026, 3, 1), license.getExpiryDate());
        assertEquals(IssuedLicense.STATUS_ACTIVE, license.getLicenseStatus());
        assertEquals("B", license.getVehicleClass());
        assertEquals("None", license.getRestrictions());
        assertEquals(0, license.getDemeritBalance());
        assertEquals("DLIS-HQ", license.getIssuedByAuthority());
        assertEquals("OFC-01", license.getIssuedByOfficer());
        assertEquals(0, license.getRenewalCount());
        assertEquals(100L, license.getPreviousLicenseId());
        assertEquals("Issued after road test", license.getNotes());
    }
}
