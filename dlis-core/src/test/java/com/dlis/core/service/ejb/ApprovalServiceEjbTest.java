package com.dlis.core.service.ejb;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.dao.IssuedLicenseDao;
import com.dlis.core.domain.AuthorityUser;
import com.dlis.core.domain.IssuedLicense;
import com.dlis.core.domain.LicenseApplication;
import com.dlis.core.service.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApprovalServiceEjb.
 * Business rules verified against AB-RECORD-APPROVAL-1, AB-RECORD-APPROVAL-2, AB-ISSUE-LICENSE.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceEjbTest {

    @Mock private ApplicationDao  applicationDao;
    @Mock private IssuedLicenseDao licenseDao;
    @Mock private DataSource       dataSource;
    @Mock private Connection       connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet         resultSet;

    @InjectMocks
    private ApprovalServiceEjb service;

    private LicenseApplication paymentCompleteApp;
    private LicenseApplication approval2PendApp;
    private LicenseApplication fullyApprovedApp;

    @BeforeEach
    void setUp() throws Exception {
        // Inject the mocked DataSource via reflection (field is @Resource, not @Inject)
        Field dsField = ApprovalServiceEjb.class.getDeclaredField("dataSource");
        dsField.setAccessible(true);
        dsField.set(service, dataSource);

        paymentCompleteApp = new LicenseApplication();
        paymentCompleteApp.setApplicationId(10L);
        paymentCompleteApp.setCandidateId(1L);
        paymentCompleteApp.setLicenseType("L");
        paymentCompleteApp.setPaymentStatus("P");
        paymentCompleteApp.setHistoryChkStatus("P");
        paymentCompleteApp.setEligibilityChkStatus("P");
        paymentCompleteApp.setApplicationStatus(LicenseApplication.STATUS_PAYMENT_APPROVED);

        approval2PendApp = new LicenseApplication();
        approval2PendApp.setApplicationId(10L);
        approval2PendApp.setCandidateId(1L);
        approval2PendApp.setLicenseType("L");
        approval2PendApp.setPaymentStatus("P");
        approval2PendApp.setHistoryChkStatus("P");
        approval2PendApp.setEligibilityChkStatus("P");
        approval2PendApp.setApplicationStatus(LicenseApplication.STATUS_APPROVAL2_PEND);
        approval2PendApp.setApproval1Status("A");

        fullyApprovedApp = new LicenseApplication();
        fullyApprovedApp.setApplicationId(10L);
        fullyApprovedApp.setCandidateId(1L);
        fullyApprovedApp.setLicenseType("L");
        fullyApprovedApp.setPaymentStatus("P");
        fullyApprovedApp.setHistoryChkStatus("P");
        fullyApprovedApp.setEligibilityChkStatus("P");
        fullyApprovedApp.setApplicationStatus(LicenseApplication.STATUS_APPROVED);
        fullyApprovedApp.setApproval1Status("A");
        fullyApprovedApp.setApproval2Status("A");
    }

    // Helper: configure dataSource to return an AuthorityUser from the mock JDBC chain
    private void stubAuthority(String userCode, String level, String licenseTypesAuthorised)
            throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("AUTHORITY_USER_ID")).thenReturn(99L);
        when(resultSet.getString("USER_CODE")).thenReturn(userCode);
        when(resultSet.getString("USER_NAME")).thenReturn("Test User");
        when(resultSet.getString("AUTHORITY_NAME")).thenReturn("Test Authority");
        when(resultSet.getString("AUTHORITY_LEVEL")).thenReturn(level);
        when(resultSet.getString("DEPARTMENT")).thenReturn("DMV");
        when(resultSet.getString("PHONE_NUMBER")).thenReturn("0111111111");
        when(resultSet.getString("EMAIL_ADDRESS")).thenReturn("test@dlis.com");
        when(resultSet.getString("ACTIVE_STATUS")).thenReturn("A");
        when(resultSet.getString("LICENSE_TYPES_AUTHORISED")).thenReturn(licenseTypesAuthorised);
    }

    // Helper: configure dataSource so the authority user is not found
    private void stubAuthorityNotFound() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
    }

    // ── recordApproval1 ───────────────────────────────────────────────────────

    @Test
    void recordApproval1_invalidDecision_returnsError() {
        // Arrange — no stubs needed; decision check is first

        // Act
        ServiceResult<Void> result = service.recordApproval1(10L, "USER1", "X", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_DECISION, result.getReturnCode());
        verify(applicationDao, never()).findById(anyLong());
    }

    @Test
    void recordApproval1_applicationNotFound_returnsError() {
        // Arrange
        when(applicationDao.findById(99L)).thenReturn(Optional.empty());

        // Act
        ServiceResult<Void> result = service.recordApproval1(99L, "USER1", "A", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    @Test
    void recordApproval1_paymentNotDone_returnsError() {
        // Arrange
        LicenseApplication unpaidApp = new LicenseApplication();
        unpaidApp.setApplicationId(10L);
        unpaidApp.setPaymentStatus("U");
        unpaidApp.setLicenseType("L");
        when(applicationDao.findById(10L)).thenReturn(Optional.of(unpaidApp));

        // Act
        ServiceResult<Void> result = service.recordApproval1(10L, "USER1", "A", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_PAYMENT_NOT_DONE, result.getReturnCode());
        assertTrue(result.getMessage().contains("PAYMENT"));
    }

    @Test
    void recordApproval1_authorityUserNotFound_returnsError() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(paymentCompleteApp));
        stubAuthorityNotFound();

        // Act
        ServiceResult<Void> result = service.recordApproval1(10L, "UNKNOWN", "A", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        assertTrue(result.getMessage().contains("AUTHORITY USER NOT FOUND"));
    }

    @Test
    void recordApproval1_authorityNotAuthorisedForLicenseType_returnsError() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(paymentCompleteApp));
        stubAuthority("USER1", "1", "O"); // authorised for Open only, app is Learner

        // Act
        ServiceResult<Void> result = service.recordApproval1(10L, "USER1", "A", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_AUTHORISED, result.getReturnCode());
        assertTrue(result.getMessage().contains("NOT AUTHORISED"));
    }

    @Test
    void recordApproval1_approveDecision_updatesStatusToA2() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(paymentCompleteApp));
        stubAuthority("USER1", "1", "LPO");

        // Act
        ServiceResult<Void> result = service.recordApproval1(10L, "USER1", "A", "notes");

        // Assert
        assertTrue(result.isOk());
        verify(applicationDao).updateApproval1(
                eq(10L), eq("A"), anyString(), any(LocalDate.class), eq("notes"),
                eq(LicenseApplication.STATUS_APPROVAL2_PEND), any(LocalDate.class), eq("USER1"));
        assertTrue(result.getMessage().contains("FIRST APPROVAL RECORDED"));
    }

    @Test
    void recordApproval1_rejectDecision_updatesStatusToRejected() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(paymentCompleteApp));
        stubAuthority("USER1", "1", "LPO");

        // Act
        ServiceResult<Void> result = service.recordApproval1(10L, "USER1", "R", "notes");

        // Assert
        assertTrue(result.isOk());
        verify(applicationDao).updateApproval1(
                eq(10L), eq("R"), anyString(), any(LocalDate.class), eq("notes"),
                eq(LicenseApplication.STATUS_REJECTED), any(LocalDate.class), eq("USER1"));
        assertTrue(result.getMessage().contains("REJECTED"));
    }

    // ── recordApproval2 ───────────────────────────────────────────────────────

    @Test
    void recordApproval2_invalidDecision_returnsError() {
        // Arrange — decision check is first

        // Act
        ServiceResult<Void> result = service.recordApproval2(10L, "USER2", "X", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_INVALID_DECISION, result.getReturnCode());
        verify(applicationDao, never()).findById(anyLong());
    }

    @Test
    void recordApproval2_applicationNotInA2Status_returnsError() {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(paymentCompleteApp));
        // paymentCompleteApp has status PA, not A2

        // Act
        ServiceResult<Void> result = service.recordApproval2(10L, "USER2", "A", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(2, result.getReturnCode());
        assertTrue(result.getMessage().contains("A2"));
    }

    @Test
    void recordApproval2_authorityUserNotFound_returnsError() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(approval2PendApp));
        stubAuthorityNotFound();

        // Act
        ServiceResult<Void> result = service.recordApproval2(10L, "UNKNOWN", "A", "notes");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    @Test
    void recordApproval2_approveDecision_updatesStatusToApproved() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(approval2PendApp));
        stubAuthority("USER2", "2", "LPO");

        // Act
        ServiceResult<Void> result = service.recordApproval2(10L, "USER2", "A", "notes");

        // Assert
        assertTrue(result.isOk());
        verify(applicationDao).updateApproval2(
                eq(10L), eq("A"), anyString(), any(LocalDate.class), eq("notes"),
                eq(LicenseApplication.STATUS_APPROVED), any(LocalDate.class), eq("USER2"));
        assertTrue(result.getMessage().contains("FULLY APPROVED"));
    }

    @Test
    void recordApproval2_rejectDecision_updatesStatusToRejected() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(approval2PendApp));
        stubAuthority("USER2", "2", "LPO");

        // Act
        ServiceResult<Void> result = service.recordApproval2(10L, "USER2", "R", "notes");

        // Assert
        assertTrue(result.isOk());
        verify(applicationDao).updateApproval2(
                eq(10L), eq("R"), anyString(), any(LocalDate.class), eq("notes"),
                eq(LicenseApplication.STATUS_REJECTED), any(LocalDate.class), eq("USER2"));
        assertTrue(result.getMessage().contains("REJECTED"));
    }

    // ── issueLicense ──────────────────────────────────────────────────────────

    @Test
    void issueLicense_invalidVehicleClass_returnsError() {
        // Arrange — vehicle class check is first

        // Act
        ServiceResult<IssuedLicense> result = service.issueLicense(10L, "X", null, "OFFICER1", "AUTH1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(3, result.getReturnCode());
        verify(applicationDao, never()).findById(anyLong());
    }

    @Test
    void issueLicense_applicationNotFound_returnsError() {
        // Arrange
        when(applicationDao.findById(99L)).thenReturn(Optional.empty());

        // Act
        ServiceResult<IssuedLicense> result = service.issueLicense(99L, "B", null, "OFFICER1", "AUTH1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
    }

    @Test
    void issueLicense_notFullyApproved_returnsError() {
        // Arrange — paymentCompleteApp has status PA, not AP
        when(applicationDao.findById(10L)).thenReturn(Optional.of(paymentCompleteApp));

        // Act
        ServiceResult<IssuedLicense> result = service.issueLicense(10L, "B", null, "OFFICER1", "AUTH1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(2, result.getReturnCode());
        assertTrue(result.getMessage().contains("FULLY APPROVED"));
    }

    @Test
    void issueLicense_success_learnerLicenseNumberAndExpiry() {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(fullyApprovedApp));

        // Act
        ServiceResult<IssuedLicense> result = service.issueLicense(10L, "B", "NONE", "OFFICER1", "AUTH1");

        // Assert
        assertTrue(result.isOk());
        IssuedLicense lic = result.getPayload();
        assertNotNull(lic);
        assertTrue(lic.getLicenseNumber().startsWith("LRNB"));
        assertEquals(LocalDate.now().plusYears(1), lic.getExpiryDate());
        assertEquals(IssuedLicense.STATUS_ACTIVE, lic.getLicenseStatus());
        assertEquals(12, lic.getDemeritBalance());
        verify(licenseDao).insert(lic);
        verify(applicationDao).updateApplicationStatus(
                eq(10L), eq(LicenseApplication.STATUS_ISSUED), any(LocalDate.class), eq("OFFICER1"));
    }

    @Test
    void issueLicense_probationLicense_expiresIn2Years() {
        // Arrange
        fullyApprovedApp.setLicenseType("P");
        when(applicationDao.findById(10L)).thenReturn(Optional.of(fullyApprovedApp));

        // Act
        ServiceResult<IssuedLicense> result = service.issueLicense(10L, "B", null, "OFFICER1", "AUTH1");

        // Assert
        assertTrue(result.isOk());
        assertEquals(LocalDate.now().plusYears(2), result.getPayload().getExpiryDate());
        assertTrue(result.getPayload().getLicenseNumber().startsWith("PRBB"));
    }

    @Test
    void issueLicense_openLicense_expiresIn5Years() {
        // Arrange
        fullyApprovedApp.setLicenseType("O");
        when(applicationDao.findById(10L)).thenReturn(Optional.of(fullyApprovedApp));

        // Act
        ServiceResult<IssuedLicense> result = service.issueLicense(10L, "B", null, "OFFICER1", "AUTH1");

        // Assert
        assertTrue(result.isOk());
        assertEquals(LocalDate.now().plusYears(5), result.getPayload().getExpiryDate());
        assertTrue(result.getPayload().getLicenseNumber().startsWith("OPNB"));
    }

    @Test
    void issueLicense_sqlExceptionInFindAuthority_isNotTriggered() {
        // issueLicense does not use JDBC directly — this is a guard test
        when(applicationDao.findById(10L)).thenReturn(Optional.of(fullyApprovedApp));

        ServiceResult<IssuedLicense> result = service.issueLicense(10L, "A", null, "OFFICER1", "AUTH1");

        assertTrue(result.isOk());
        verifyNoInteractions(dataSource);
    }
}
