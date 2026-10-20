package com.dlis.core.service.ejb;

import com.dlis.core.dao.ApplicationDao;
import com.dlis.core.dao.LicenseFeeScheduleDao;
import com.dlis.core.domain.LicenseApplication;
import com.dlis.core.domain.LicenseFeeSchedule;
import com.dlis.core.domain.Payment;
import com.dlis.core.service.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceEjb.
 * Business rules verified against AB-PROCESS-PAYMENT.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceEjbTest {

    @Mock private ApplicationDao       applicationDao;
    @Mock private LicenseFeeScheduleDao feeDao;
    @Mock private DataSource            dataSource;
    @Mock private Connection            connection;
    @Mock private PreparedStatement     preparedStatement;
    @Mock private ResultSet             generatedKeys;

    @InjectMocks
    private PaymentServiceEjb service;

    private LicenseApplication historyPassedApp;
    private LicenseFeeSchedule feeSchedule;

    @BeforeEach
    void setUp() throws Exception {
        // Inject the mocked DataSource via reflection (field is @Resource, not @Inject)
        Field dsField = PaymentServiceEjb.class.getDeclaredField("dataSource");
        dsField.setAccessible(true);
        dsField.set(service, dataSource);

        historyPassedApp = new LicenseApplication();
        historyPassedApp.setApplicationId(10L);
        historyPassedApp.setCandidateId(1L);
        historyPassedApp.setLicenseType("L");
        historyPassedApp.setHistoryChkStatus("P");
        historyPassedApp.setPaymentStatus("U");

        feeSchedule = new LicenseFeeSchedule();
        feeSchedule.setLicenseType("L");
        feeSchedule.setFeeType("IF");
        feeSchedule.setFeeAmount(new BigDecimal("250.00"));
    }

    // ── processPayment ────────────────────────────────────────────────────────

    @Test
    void processPayment_applicationNotFound_returnsError() {
        // Arrange
        when(applicationDao.findById(99L)).thenReturn(Optional.empty());

        // Act
        ServiceResult<Payment> result = service.processPayment(99L, 1L, "CC", "REF001", "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        verify(applicationDao, never()).updatePayment(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void processPayment_historyNotPassed_returnsError() {
        // Arrange
        LicenseApplication app = new LicenseApplication();
        app.setApplicationId(10L);
        app.setHistoryChkStatus("U");
        app.setPaymentStatus("U");
        when(applicationDao.findById(10L)).thenReturn(Optional.of(app));

        // Act
        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_HISTORY_NOT_PASSED, result.getReturnCode());
        assertTrue(result.getMessage().contains("HISTORY"));
        verify(applicationDao, never()).updatePayment(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void processPayment_alreadyPaid_returnsError() {
        // Arrange
        LicenseApplication app = new LicenseApplication();
        app.setApplicationId(10L);
        app.setHistoryChkStatus("P");
        app.setPaymentStatus("P");
        when(applicationDao.findById(10L)).thenReturn(Optional.of(app));

        // Act
        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_ALREADY_PAID, result.getReturnCode());
        assertTrue(result.getMessage().contains("ALREADY BEEN RECORDED"));
        verify(applicationDao, never()).updatePayment(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void processPayment_feeNotFound_returnsError() {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.empty());

        // Act
        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        // Assert
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_FEE_NOT_FOUND, result.getReturnCode());
        assertTrue(result.getMessage().contains("FEE SCHEDULE"));
        verify(applicationDao, never()).updatePayment(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void processPayment_success_receiptNumberFormat() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.of(feeSchedule));
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getLong(1)).thenReturn(55L);

        // Act
        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        // Assert
        assertTrue(result.isOk());
        Payment payment = result.getPayload();
        assertNotNull(payment);
        String expectedPrefix = "RCP" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "10";
        assertEquals(expectedPrefix, payment.getReceiptNumber());
        assertEquals(new BigDecimal("250.00"), payment.getPaymentAmount());
        assertEquals(55L, payment.getPaymentId());
    }

    @Test
    void processPayment_success_updatePaymentCalledWithCorrectStatus() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.of(feeSchedule));
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false);

        // Act
        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        // Assert
        assertTrue(result.isOk());
        verify(applicationDao).updatePayment(
                eq(10L), eq("P"), eq("REF001"),
                eq(LicenseApplication.STATUS_PAYMENT_APPROVED),
                any(LocalDate.class), eq("OFFICER1"));
    }

    @Test
    void processPayment_sqlException_throwsRuntimeException() throws Exception {
        // Arrange
        when(applicationDao.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.of(feeSchedule));
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("DB error"));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1"));
        assertTrue(ex.getMessage().contains("insertPayment"));
    }
}
