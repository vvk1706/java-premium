package com.dlis.core.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorityUserTest {

    private AuthorityUser user;

    @BeforeEach
    void setUp() {
        user = new AuthorityUser();
        user.setAuthorityUserId(1L);
        user.setUserCode("U001");
        user.setUserName("Alice Approver");
        user.setAuthorityName("Licensing Authority");
        user.setDepartment("Traffic");
        user.setPhoneNumber("0110001111");
        user.setEmailAddress("alice@dlis.gov");
        user.setLicenseTypesAuthorised("LPO");
    }

    // ── isActive ──────────────────────────────────────────────────────────────

    @Test
    void isActive_activeStatus_returnsTrue() {
        // Arrange
        user.setActiveStatus("A");

        // Act
        boolean result = user.isActive();

        // Assert
        assertTrue(result);
    }

    @Test
    void isActive_inactiveStatus_returnsFalse() {
        // Arrange
        user.setActiveStatus("I");

        // Act
        boolean result = user.isActive();

        // Assert
        assertFalse(result);
    }

    @Test
    void isActive_nullStatus_returnsFalse() {
        // Arrange
        user.setActiveStatus(null);

        // Act
        boolean result = user.isActive();

        // Assert
        assertFalse(result);
    }

    // ── isAuthorisedFor ───────────────────────────────────────────────────────

    @Test
    void isAuthorisedFor_typeContainedInList_returnsTrue() {
        // Arrange
        user.setLicenseTypesAuthorised("LPO");

        // Act & Assert
        assertTrue(user.isAuthorisedFor("L"));
        assertTrue(user.isAuthorisedFor("P"));
        assertTrue(user.isAuthorisedFor("O"));
    }

    @Test
    void isAuthorisedFor_typeNotInList_returnsFalse() {
        // Arrange
        user.setLicenseTypesAuthorised("L");

        // Act & Assert
        assertFalse(user.isAuthorisedFor("P"));
        assertFalse(user.isAuthorisedFor("O"));
    }

    @Test
    void isAuthorisedFor_nullAuthorisedTypes_returnsFalse() {
        // Arrange
        user.setLicenseTypesAuthorised(null);

        // Act
        boolean result = user.isAuthorisedFor("L");

        // Assert
        assertFalse(result);
    }

    // ── isLevel1 / isLevel2 ───────────────────────────────────────────────────

    @Test
    void isLevel1_levelOne_returnsTrue() {
        // Arrange
        user.setAuthorityLevel("1");

        // Act & Assert
        assertTrue(user.isLevel1());
        assertFalse(user.isLevel2());
    }

    @Test
    void isLevel2_levelTwo_returnsTrue() {
        // Arrange
        user.setAuthorityLevel("2");

        // Act & Assert
        assertTrue(user.isLevel2());
        assertFalse(user.isLevel1());
    }

    @Test
    void isLevel1_nullLevel_returnsFalse() {
        // Arrange
        user.setAuthorityLevel(null);

        // Act & Assert
        assertFalse(user.isLevel1());
        assertFalse(user.isLevel2());
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Assert (set via setUp)
        assertEquals(1L, user.getAuthorityUserId());
        assertEquals("U001", user.getUserCode());
        assertEquals("Alice Approver", user.getUserName());
        assertEquals("Licensing Authority", user.getAuthorityName());
        assertEquals("Traffic", user.getDepartment());
        assertEquals("0110001111", user.getPhoneNumber());
        assertEquals("alice@dlis.gov", user.getEmailAddress());
        assertEquals("LPO", user.getLicenseTypesAuthorised());
    }
}
