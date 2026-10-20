package com.dlis.cics.program;

import com.dlis.core.domain.IssuedLicense;
import com.dlis.core.domain.Payment;
import com.dlis.core.service.ApprovalService;
import com.dlis.core.service.PaymentService;
import com.dlis.core.service.ServiceResult;
import com.ibm.cics.server.CommAreaHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DlisPayApprProgram — CICS entry point for payment and approval.
 * Covers sub-functions PAYM, APR1, APR2, ISSU and error paths.
 *
 * COMMAREA layout (544 bytes):
 *   Offset   0:  4  FUNCTION (PAYM/APR1/APR2/ISSU)
 *   Offset   4:  4  RETURN-CODE
 *   Offset   8:100  RETURN-MESSAGE
 *   Offset 108: 20  USER-ID
 *   Offset 128: 10  APPLICATION-ID
 *   Offset 138: 10  CANDIDATE-ID
 *   Offset 148:  2  PAYMENT-METHOD
 *   Offset 150: 30  PAYMENT-REFERENCE
 *   Offset 180: 20  RECEIPT-NUMBER
 *   Offset 200: 10  FEE-AMOUNT
 *   Offset 210:  1  DECISION
 *   Offset 211:200  DECISION-NOTES
 *   Offset 411:  2  VEHICLE-CLASS
 *   Offset 413: 20  LICENSE-NUMBER
 *   Offset 433: 10  LICENSE-ID
 *   Offset 443: 50  ISSUED-BY-AUTHORITY
 *   Offset 493: 50  ISSUED-BY-OFFICER
 */
@ExtendWith(MockitoExtension.class)
class DlisPayApprProgramTest {

    @Mock private PaymentService  paymentService;
    @Mock private ApprovalService approvalService;

    @InjectMocks
    private DlisPayApprProgram program;

    private static final int BUF_SIZE = 544;

    // ─── byte-level helpers (mirrors program internals) ──────────────────────

    private static byte[] blankBuffer() {
        byte[] buf = new byte[BUF_SIZE];
        Arrays.fill(buf, (byte) 0x20);
        return buf;
    }

    private static void encode(byte[] buf, int off, String val, int len) {
        byte[] space = new byte[len];
        Arrays.fill(space, (byte) 0x20);
        if (val != null) {
            byte[] src = val.getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(src, 0, space, 0, Math.min(src.length, len));
        }
        System.arraycopy(space, 0, buf, off, len);
    }

    private static void encodeNum(byte[] buf, int off, long val, int len) {
        encode(buf, off, String.format("%0" + len + "d", val), len);
    }

    private static String decode(byte[] buf, int off, int len) {
        return new String(buf, off, len, StandardCharsets.ISO_8859_1).trim();
    }

    private static int readCode(byte[] buf) {
        return (int) Long.parseLong(decode(buf, 4, 4));
    }

    private static String readMsg(byte[] buf) {
        return decode(buf, 8, 100);
    }

    private CommAreaHolder holderWith(byte[] raw) {
        CommAreaHolder h = mock(CommAreaHolder.class);
        when(h.getValue()).thenReturn(raw);
        return h;
    }

    private byte[] captureOut(CommAreaHolder holder) {
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(holder).setValue(captor.capture());
        return captor.getValue();
    }

    // ── null / empty CommAreaHolder ───────────────────────────────────────────

    @Test
    void execute_nullCommAreaHolder_doesNotThrow() {
        // Arrange (nothing)

        // Act / Assert
        assertDoesNotThrow(() -> program.execute(null));
    }

    @Test
    void execute_nullCommAreaValue_doesNotThrow() throws Exception {
        // Arrange
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(null);

        // Act / Assert
        assertDoesNotThrow(() -> program.execute(holder));
    }

    // ── PAYM — process payment ────────────────────────────────────────────────

    @Test
    void execute_paymSuccess_writesReceiptAndAmountAndRc0() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "PAYM",    4);
        encode(raw, 108, "OFFICER1", 20);
        encodeNum(raw, 128, 100L,   10);   // APPLICATION-ID
        encodeNum(raw, 138,   1L,   10);   // CANDIDATE-ID
        encode(raw, 148, "CC",       2);   // PAYMENT-METHOD
        encode(raw, 150, "REF12345", 30);  // PAYMENT-REFERENCE

        Payment payment = new Payment();
        payment.setReceiptNumber("RCP2024010100100");
        payment.setPaymentAmount(new BigDecimal("150.00"));

        when(paymentService.processPayment(100L, 1L, "CC", "REF12345", "OFFICER1"))
                .thenReturn(ServiceResult.ok(payment, "PAYMENT OK"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(0, readCode(out));
        assertEquals("RCP2024010100100", decode(out, 180, 20));
        assertEquals("0000015000", decode(out, 200, 10));   // 150.00 * 100 = 15000, zero-padded to 10
    }

    @Test
    void execute_paymServiceError_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "PAYM",    4);
        encodeNum(raw, 128, 100L,   10);
        encodeNum(raw, 138,   1L,   10);
        encode(raw, 148, "CC",       2);
        encode(raw, 108, "OFFICER1", 20);

        when(paymentService.processPayment(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(ServiceResult.error(ServiceResult.RC_ALREADY_PAID, "ALREADY PAID"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(ServiceResult.RC_ALREADY_PAID, readCode(out));
        assertTrue(readMsg(out).contains("ALREADY PAID"));
    }

    @Test
    void execute_paymSuccessNullAmount_doesNotThrow() throws Exception {
        // Arrange — payment amount is null; the program guards with a null check
        byte[] raw = blankBuffer();
        encode(raw,   0, "PAYM",    4);
        encodeNum(raw, 128, 100L,   10);
        encodeNum(raw, 138,   1L,   10);
        encode(raw, 148, "CC",       2);
        encode(raw, 108, "OFFICER1", 20);

        Payment payment = new Payment();
        payment.setReceiptNumber("RCP001");
        payment.setPaymentAmount(null);   // edge case

        when(paymentService.processPayment(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(ServiceResult.ok(payment, "PAYMENT OK"));

        CommAreaHolder holder = holderWith(raw);

        // Act / Assert — must not throw
        assertDoesNotThrow(() -> program.execute(holder));
        assertEquals(0, readCode(captureOut(holder)));
    }

    // ── APR1 — record approval 1 ──────────────────────────────────────────────

    @Test
    void execute_apr1Success_writesRc0() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "APR1",    4);
        encode(raw, 108, "AUTH001", 20);
        encodeNum(raw, 128, 100L,   10);
        encode(raw, 210, "A",        1);
        encode(raw, 211, "APPROVED",200);

        when(approvalService.recordApproval1(100L, "AUTH001", "A", "APPROVED"))
                .thenReturn(ServiceResult.ok(null, "APPROVAL1 RECORDED"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(0, readCode(out));
    }

    @Test
    void execute_apr1Unauthorised_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "APR1",    4);
        encode(raw, 108, "AUTH001", 20);
        encodeNum(raw, 128, 100L,   10);
        encode(raw, 210, "A",        1);

        when(approvalService.recordApproval1(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(ServiceResult.error(ServiceResult.RC_NOT_AUTHORISED, "NOT AUTHORISED"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(ServiceResult.RC_NOT_AUTHORISED, readCode(out));
        assertTrue(readMsg(out).contains("NOT AUTHORISED"));
    }

    // ── APR2 — record approval 2 ──────────────────────────────────────────────

    @Test
    void execute_apr2Success_writesRc0() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "APR2",    4);
        encode(raw, 108, "AUTH002", 20);
        encodeNum(raw, 128, 100L,   10);
        encode(raw, 210, "A",        1);
        encode(raw, 211, "APPROVED",200);

        when(approvalService.recordApproval2(100L, "AUTH002", "A", "APPROVED"))
                .thenReturn(ServiceResult.ok(null, "APPROVAL2 RECORDED"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(0, readCode(out));
    }

    @Test
    void execute_apr2Rejected_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "APR2",    4);
        encode(raw, 108, "AUTH002", 20);
        encodeNum(raw, 128, 100L,   10);
        encode(raw, 210, "R",        1);

        when(approvalService.recordApproval2(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(ServiceResult.error(ServiceResult.RC_INVALID_DECISION, "INVALID DECISION"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(ServiceResult.RC_INVALID_DECISION, readCode(out));
    }

    // ── ISSU — issue license ──────────────────────────────────────────────────

    @Test
    void execute_issuSuccess_writesLicenseNumberAndId() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "ISSU",          4);
        encodeNum(raw, 128, 100L,         10);
        encode(raw, 411, "B",              2);
        encode(raw, 211, "NONE",         200);
        encode(raw, 493, "OFFICER SMITH",  50);
        encode(raw, 443, "DMV CENTRAL",    50);

        IssuedLicense lic = new IssuedLicense();
        lic.setLicenseId(5001L);
        lic.setLicenseNumber("LIC-2024-001");

        when(approvalService.issueLicense(100L, "B", "NONE", "OFFICER SMITH", "DMV CENTRAL"))
                .thenReturn(ServiceResult.ok(lic, "LICENSE ISSUED"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(0, readCode(out));
        assertEquals("LIC-2024-001", decode(out, 413, 20));
        assertEquals("0000005001", decode(out, 433, 10));
    }

    @Test
    void execute_issuPaymentNotDone_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "ISSU",  4);
        encodeNum(raw, 128, 100L, 10);

        when(approvalService.issueLicense(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ServiceResult.error(ServiceResult.RC_PAYMENT_NOT_DONE, "PAYMENT NOT DONE"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(ServiceResult.RC_PAYMENT_NOT_DONE, readCode(out));
        assertTrue(readMsg(out).contains("PAYMENT NOT DONE"));
    }

    // ── Unknown function code ─────────────────────────────────────────────────

    @Test
    void execute_unknownFunction_writesRc99() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw, 0, "ZZZZ", 4);

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(99, readCode(out));
        assertTrue(readMsg(out).contains("UNKNOWN FUNCTION"));
        verifyNoInteractions(paymentService, approvalService);
    }

    // ── Exception handling ────────────────────────────────────────────────────

    @Test
    void execute_serviceThrowsException_writesRc99() throws Exception {
        // Arrange
        byte[] raw = blankBuffer();
        encode(raw,   0, "PAYM",    4);
        encodeNum(raw, 128, 100L,   10);
        encodeNum(raw, 138,   1L,   10);
        encode(raw, 148, "CC",       2);
        encode(raw, 108, "OFFICER1", 20);

        when(paymentService.processPayment(anyLong(), anyLong(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected failure"));

        CommAreaHolder holder = holderWith(raw);

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        assertEquals(99, readCode(out));
        assertTrue(readMsg(out).contains("INTERNAL ERROR"));
    }
}
