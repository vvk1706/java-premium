package com.dlis.payment.service.ejb;

import com.dlis.common.RemoteServiceException;
import com.dlis.common.ServiceResult;
import com.dlis.payment.client.ApplicationGateway;
import com.dlis.payment.dao.LicenseFeeScheduleDao;
import com.dlis.payment.dao.PaymentDao;
import com.dlis.payment.domain.ApplicationRef;
import com.dlis.payment.domain.LicenseFeeSchedule;
import com.dlis.payment.domain.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ejb.SessionContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceEjb in the payment microservice.
 * Business rules verified against AB-PROCESS-PAYMENT; the application state now comes
 * through ApplicationGateway instead of a shared table.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceEjbTest {

    @Mock private ApplicationGateway    applicationGateway;
    @Mock private LicenseFeeScheduleDao feeDao;
    @Mock private PaymentDao            paymentDao;
    @Mock private SessionContext        sessionContext;

    @InjectMocks
    private PaymentServiceEjb service;

    private ApplicationRef historyPassedApp;
    private LicenseFeeSchedule feeSchedule;

    @BeforeEach
    void setUp() {
        historyPassedApp = new ApplicationRef(10L, "L", true, false);
        feeSchedule = new LicenseFeeSchedule();
        feeSchedule.setLicenseType("L");
        feeSchedule.setFeeType("IF");
        feeSchedule.setFeeAmount(new BigDecimal("250.00"));
    }

    @Test
    void processPayment_applicationNotFound_returnsError() {
        when(applicationGateway.findById(99L)).thenReturn(Optional.empty());

        ServiceResult<Payment> result = service.processPayment(99L, 1L, "CC", "REF001", "OFFICER1");

        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        verify(paymentDao, never()).insert(any());
        verify(applicationGateway, never()).paymentRecorded(any(), any(), any());
    }

    @Test
    void processPayment_historyNotPassed_returnsError() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(new ApplicationRef(10L, "L", false, false)));

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertEquals(ServiceResult.RC_HISTORY_NOT_PASSED, result.getReturnCode());
        assertTrue(result.getMessage().contains("HISTORY"));
        verify(paymentDao, never()).insert(any());
    }

    @Test
    void processPayment_alreadyPaid_returnsError() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(new ApplicationRef(10L, "L", true, true)));

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertEquals(ServiceResult.RC_ALREADY_PAID, result.getReturnCode());
        assertTrue(result.getMessage().contains("ALREADY BEEN RECORDED"));
        verify(paymentDao, never()).insert(any());
    }

    @Test
    void processPayment_feeNotFound_returnsError() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.empty());

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertEquals(ServiceResult.RC_FEE_NOT_FOUND, result.getReturnCode());
        assertTrue(result.getMessage().contains("FEE SCHEDULE"));
        verify(paymentDao, never()).insert(any());
    }

    @Test
    void processPayment_success_persistsPaymentAndNotifiesLicensing() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.of(feeSchedule));
        doAnswer(inv -> { ((Payment) inv.getArgument(0)).setPaymentId(55L); return null; })
                .when(paymentDao).insert(any());
        when(applicationGateway.paymentRecorded(10L, "REF001", "OFFICER1"))
                .thenReturn(ServiceResult.ok(null, "OK"));

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertTrue(result.isOk());
        Payment payment = result.getPayload();
        String expectedReceipt = "RCP" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "10";
        assertEquals(expectedReceipt, payment.getReceiptNumber());
        assertEquals(new BigDecimal("250.00"), payment.getPaymentAmount());
        assertEquals(55L, payment.getPaymentId());
        assertEquals("S", payment.getPaymentStatus());
        assertEquals("IF", payment.getFeeType());

        ArgumentCaptor<Payment> cap = ArgumentCaptor.forClass(Payment.class);
        verify(paymentDao).insert(cap.capture());
        assertEquals("REF001", cap.getValue().getPaymentReference());
        verify(applicationGateway).paymentRecorded(10L, "REF001", "OFFICER1");
        verify(sessionContext, never()).setRollbackOnly();
    }

    @Test
    void processPayment_licensingRejects_marksRollbackAndKeepsErrorCode() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.of(feeSchedule));
        when(applicationGateway.paymentRecorded(10L, "REF001", "OFFICER1"))
                .thenReturn(ServiceResult.error(ServiceResult.RC_ALREADY_PAID, "ALREADY PAID"));

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertEquals(ServiceResult.RC_ALREADY_PAID, result.getReturnCode());
        verify(sessionContext).setRollbackOnly();
    }

    @Test
    void processPayment_licensingUnreachable_propagatesForRollback() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(feeDao.findActiveFee("L", "IF")).thenReturn(Optional.of(feeSchedule));
        when(applicationGateway.paymentRecorded(any(), any(), any()))
                .thenThrow(new RemoteServiceException("licensing down"));

        assertThrows(RemoteServiceException.class,
                () -> service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1"));
    }

    @Test
    void processPayment_retryWithSameReference_isIdempotent() {
        Payment already = new Payment();
        already.setPaymentId(55L);
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(paymentDao.findByApplicationAndReference(10L, "REF001")).thenReturn(Optional.of(already));
        when(applicationGateway.paymentRecorded(10L, "REF001", "OFFICER1"))
                .thenReturn(ServiceResult.ok(null, "OK"));

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertTrue(result.isOk());
        assertSame(already, result.getPayload());
        verify(paymentDao, never()).insert(any());
    }

    @Test
    void processPayment_retryButLicensingStillRejects_returnsLicensingError() {
        when(applicationGateway.findById(10L)).thenReturn(Optional.of(historyPassedApp));
        when(paymentDao.findByApplicationAndReference(10L, "REF001")).thenReturn(Optional.of(new Payment()));
        when(applicationGateway.paymentRecorded(any(), any(), any()))
                .thenReturn(ServiceResult.error(ServiceResult.RC_HISTORY_NOT_PASSED, "NO"));

        ServiceResult<Payment> result = service.processPayment(10L, 1L, "CC", "REF001", "OFFICER1");

        assertEquals(ServiceResult.RC_HISTORY_NOT_PASSED, result.getReturnCode());
    }
}
