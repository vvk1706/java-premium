package com.dlis.candidate.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CandidateTest {

    private Candidate candidate;

    @BeforeEach
    void setUp() {
        candidate = new Candidate();
        candidate.setCandidateId(10L);
        candidate.setFirstName("John");
        candidate.setLastName("Doe");
        candidate.setDateOfBirth(LocalDate.of(1995, 3, 20));
        candidate.setIdNumber("SA9503200001");
        candidate.setAddressLine1("45 Oak Avenue");
        candidate.setAddressLine2("Apt 2");
        candidate.setCity("Cape Town");
        candidate.setStateProvince("Western Cape");
        candidate.setPostalCode("8001");
        candidate.setCountry("ZA");
        candidate.setPhoneNumber("0821234567");
        candidate.setEmailAddress("john.doe@example.com");
        candidate.setCreatedDate(LocalDate.of(2024, 1, 10));
    }

    // ── isActive ──────────────────────────────────────────────────────────────

    @Test
    void isActive_activeStatus_returnsTrue() {
        // Arrange
        candidate.setRecordStatus("A");

        // Act
        boolean result = candidate.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_inactiveStatus_returnsFalse() {
        // Arrange
        candidate.setRecordStatus("I");

        // Act
        boolean result = candidate.isActive();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActive_nullStatus_returnsFalse() {
        // Arrange
        candidate.setRecordStatus(null);

        // Act
        boolean result = candidate.isActive();

        // Assert
        assertFalse(result);
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Assert (all set via setUp)
        assertEquals(10L, candidate.getCandidateId());
        assertEquals("John", candidate.getFirstName());
        assertEquals("Doe", candidate.getLastName());
        assertEquals(LocalDate.of(1995, 3, 20), candidate.getDateOfBirth());
        assertEquals("SA9503200001", candidate.getIdNumber());
        assertEquals("45 Oak Avenue", candidate.getAddressLine1());
        assertEquals("Apt 2", candidate.getAddressLine2());
        assertEquals("Cape Town", candidate.getCity());
        assertEquals("Western Cape", candidate.getStateProvince());
        assertEquals("8001", candidate.getPostalCode());
        assertEquals("ZA", candidate.getCountry());
        assertEquals("0821234567", candidate.getPhoneNumber());
        assertEquals("john.doe@example.com", candidate.getEmailAddress());
        assertEquals(LocalDate.of(2024, 1, 10), candidate.getCreatedDate());
    }
}
