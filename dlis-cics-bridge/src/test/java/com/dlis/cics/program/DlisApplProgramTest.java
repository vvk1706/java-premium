package com.dlis.cics.program;

import com.dlis.cics.commarea.ApplicationCommarea;
import com.dlis.core.domain.LicenseApplication;
import com.dlis.core.service.ApplicationService;
import com.dlis.core.service.ServiceResult;
import com.ibm.cics.server.CommAreaHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DlisApplProgram — CICS entry point for application workflow.
 * Covers sub-functions APLN, APEC, APHC, APIS and error paths.
 */
@ExtendWith(MockitoExtension.class)
class DlisApplProgramTest {

    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private DlisApplProgram program;

    /** Build a minimal 180-byte COMMAREA with just the tran-code set. */
    private byte[] buildCommArea(String tranCode, long applicationId, long candidateId,
                                  String licenseType, String userId) {
        ApplicationCommarea ca = new ApplicationCommarea();
        ca.setTranCode(tranCode);
        ca.setApplicationId(applicationId);
        ca.setCandidateId(candidateId);
        ca.setLicenseType(licenseType);
        ca.setUserId(userId);
        return ca.toBytes();
    }

    // ── null / empty CommAreaHolder ───────────────────────────────────────────

    @Test
    void execute_nullCommAreaHolder_doesNotThrow() throws Exception {
        // Arrange (nothing)

        // Act / Assert — must not throw
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

    // ── APLN — create application ─────────────────────────────────────────────

    @Test
    void execute_aplnSuccess_writesApplicationIdAndRc0() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APLN", 0L, 1L, "L", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.createApplication(1L, "L", "OFFICER1"))
                .thenReturn(ServiceResult.ok(42L, "CREATED"));

        // Act
        program.execute(holder);

        // Assert
        byte[] out = captureOut(holder);
        ApplicationCommarea result = fromBytes(out);
        assertEquals(0, result.getReturnCode());
        assertEquals(42L, result.getApplicationId());
    }

    @Test
    void execute_aplnServiceError_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APLN", 0L, 1L, "L", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.createApplication(anyLong(), anyString(), anyString()))
                .thenReturn(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "CANDIDATE NOT FOUND"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        assertTrue(result.getReturnMessage().contains("CANDIDATE NOT FOUND"));
    }

    // ── APEC — eligibility check ──────────────────────────────────────────────

    @Test
    void execute_apecSuccess_writesEligibilityStatusAndRc0() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APEC", 100L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.checkEligibility(100L, "OFFICER1"))
                .thenReturn(ServiceResult.ok("P", "ELIGIBLE"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(0, result.getReturnCode());
        assertEquals("P", result.getEligibilityStatus());
    }

    @Test
    void execute_apecServiceError_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APEC", 100L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.checkEligibility(100L, "OFFICER1"))
                .thenReturn(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    // ── APHC — history check ──────────────────────────────────────────────────

    @Test
    void execute_aphcSuccess_writesHistoryStatusAndRc0() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APHC", 100L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.checkHistory(100L, "OFFICER1"))
                .thenReturn(ServiceResult.ok("P", "HISTORY PASSED"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(0, result.getReturnCode());
        assertEquals("P", result.getHistoryStatus());
    }

    @Test
    void execute_aphcServiceError_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APHC", 100L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.checkHistory(100L, "OFFICER1"))
                .thenReturn(ServiceResult.error(ServiceResult.RC_HISTORY_NOT_PASSED, "HISTORY FAIL"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(ServiceResult.RC_HISTORY_NOT_PASSED, result.getReturnCode());
    }

    // ── APIS — status inquiry ─────────────────────────────────────────────────

    @Test
    void execute_apisFound_writesAllStatusFields() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APIS", 100L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);

        LicenseApplication app = new LicenseApplication();
        app.setApplicationId(100L);
        app.setCandidateId(1L);
        app.setLicenseType("L");
        app.setApplicationStatus(LicenseApplication.STATUS_PENDING);
        app.setEligibilityChkStatus("P");
        app.setHistoryChkStatus("P");
        app.setPaymentStatus("U");
        app.setApproval1Status("U");
        app.setApproval2Status("U");
        app.setPaymentReference("REF001");

        when(applicationService.inquireStatus(100L))
                .thenReturn(ServiceResult.ok(app, "OK"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(0, result.getReturnCode());
        assertEquals(1L, result.getCandidateId());
        assertEquals("L", result.getLicenseType());
        assertEquals(LicenseApplication.STATUS_PENDING, result.getApplicationStatus());
        assertEquals("P", result.getEligibilityStatus());
        assertEquals("P", result.getHistoryStatus());
    }

    @Test
    void execute_apisNotFound_writesNonZeroRc() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APIS", 999L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.inquireStatus(999L))
                .thenReturn(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "NOT FOUND"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    // ── Unknown function code ─────────────────────────────────────────────────

    @Test
    void execute_unknownTranCode_writesRc99() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("XXXX", 0L, 0L, "", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(99, result.getReturnCode());
        assertTrue(result.getReturnMessage().contains("UNKNOWN"));
        verifyNoInteractions(applicationService);
    }

    // ── Exception handling ────────────────────────────────────────────────────

    @Test
    void execute_serviceThrowsException_writesRc99() throws Exception {
        // Arrange
        byte[] inBytes = buildCommArea("APLN", 0L, 1L, "L", "OFFICER1");
        CommAreaHolder holder = mock(CommAreaHolder.class);
        when(holder.getValue()).thenReturn(inBytes);
        when(applicationService.createApplication(anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("DB connection failed"));

        // Act
        program.execute(holder);

        // Assert
        ApplicationCommarea result = fromBytes(captureOut(holder));
        assertEquals(99, result.getReturnCode());
        assertTrue(result.getReturnMessage().contains("INTERNAL ERROR"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] captureOut(CommAreaHolder holder) {
        // Retrieve the bytes written back via setValue
        org.mockito.ArgumentCaptor<byte[]> captor =
                org.mockito.ArgumentCaptor.forClass(byte[].class);
        verify(holder).setValue(captor.capture());
        return captor.getValue();
    }

    private ApplicationCommarea fromBytes(byte[] bytes) {
        ApplicationCommarea ca = new ApplicationCommarea();
        ca.fromBytes(bytes);
        return ca;
    }
}
