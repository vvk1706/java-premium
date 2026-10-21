package com.dlis.payment.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment();
    }

    // ── Getters / Setters round-trip ─────────────────────────────────────────

    @Test
    void gettersSetters_roundTrip_retainValues() {
        // Arrange
        LocalDate payDate = LocalDate.of(2024, 6, 15);
        payment.setPaymentId(10L);
        payment.setApplicationId(200L);
        payment.setCandidateId(300L);
        payment.setPaymentDate(payDate);
        payment.setPaymentAmount(new BigDecimal("150.00"));
        payment.setPaymentMethod("CC");
        payment.setPaymentReference("REF-001");
        payment.setPaymentStatus("S");
        payment.setLicenseType("L");
        payment.setFeeType("IF");
        payment.setReceiptNumber("RCP20240615200");
        payment.setProcessedBy("OFFICER1");
        payment.setNotes("First-time applicant payment");

        // Act — no separate act; getters are the act for round-trip tests

        // Assert
        assertEquals(10L, payment.getPaymentId());
        assertEquals(200L, payment.getApplicationId());
        assertEquals(300L, payment.getCandidateId());
        assertEquals(payDate, payment.getPaymentDate());
        assertEquals(new BigDecimal("150.00"), payment.getPaymentAmount());
        assertEquals("CC", payment.getPaymentMethod());
        assertEquals("REF-001", payment.getPaymentReference());
        assertEquals("S", payment.getPaymentStatus());
        assertEquals("L", payment.getLicenseType());
        assertEquals("IF", payment.getFeeType());
        assertEquals("RCP20240615200", payment.getReceiptNumber());
        assertEquals("OFFICER1", payment.getProcessedBy());
        assertEquals("First-time applicant payment", payment.getNotes());
    }

    @Test
    void gettersSetters_nullValues_retainNulls() {
        // Arrange
        payment.setPaymentId(null);
        payment.setApplicationId(null);
        payment.setCandidateId(null);
        payment.setPaymentDate(null);
        payment.setPaymentAmount(null);
        payment.setPaymentMethod(null);
        payment.setPaymentReference(null);
        payment.setPaymentStatus(null);
        payment.setLicenseType(null);
        payment.setFeeType(null);
        payment.setReceiptNumber(null);
        payment.setProcessedBy(null);
        payment.setNotes(null);

        // Assert
        assertNull(payment.getPaymentId());
        assertNull(payment.getApplicationId());
        assertNull(payment.getCandidateId());
        assertNull(payment.getPaymentDate());
        assertNull(payment.getPaymentAmount());
        assertNull(payment.getPaymentMethod());
        assertNull(payment.getPaymentReference());
        assertNull(payment.getPaymentStatus());
        assertNull(payment.getLicenseType());
        assertNull(payment.getFeeType());
        assertNull(payment.getReceiptNumber());
        assertNull(payment.getProcessedBy());
        assertNull(payment.getNotes());
    }
}
