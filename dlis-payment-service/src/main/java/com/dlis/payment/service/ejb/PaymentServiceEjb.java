package com.dlis.payment.service.ejb;

import com.dlis.common.ServiceResult;
import com.dlis.payment.client.ApplicationGateway;
import com.dlis.payment.dao.LicenseFeeScheduleDao;
import com.dlis.payment.dao.PaymentDao;
import com.dlis.payment.domain.ApplicationRef;
import com.dlis.payment.domain.LicenseFeeSchedule;
import com.dlis.payment.domain.Payment;
import com.dlis.payment.service.PaymentService;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * EJB implementation of PaymentService (AB-PROCESS-PAYMENT).
 *
 * Saga: (1) read application state from dlis-licensing-service, (2) insert PAYMENT locally,
 * (3) call licensing "payment-recorded". If step 3 is unreachable, RemoteServiceException (an EJB
 * application exception with rollback=true) rolls step 2 back; if licensing rejects the payment,
 * the transaction is marked rollback-only. Either way nothing is left half-done, and a retry
 * with the same paymentReference is idempotent on both sides.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class PaymentServiceEjb implements PaymentService {

    @Inject private ApplicationGateway   applicationGateway;
    @Inject private LicenseFeeScheduleDao feeDao;
    @Inject private PaymentDao           paymentDao;

    @Resource private SessionContext     sessionContext;

    @Override
    public ServiceResult<Payment> processPayment(Long applicationId,
                                                  Long candidateId,
                                                  String paymentMethod,
                                                  String paymentReference,
                                                  String processedBy) {
        // 1. Load application (owned by dlis-licensing-service)
        Optional<ApplicationRef> appOpt = applicationGateway.findById(applicationId);
        if (appOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_NOT_FOUND, "APPLICATION NOT FOUND");
        }
        ApplicationRef app = appOpt.get();

        // 2. Idempotent retry: this exact payment already exists -> just re-send the saga callback
        Optional<Payment> existing = paymentDao.findByApplicationAndReference(applicationId, paymentReference);
        if (existing.isPresent()) {
            ServiceResult<Void> ack = applicationGateway.paymentRecorded(applicationId, paymentReference, processedBy);
            return ack.isOk()
                    ? ServiceResult.ok(existing.get(), "PAYMENT RECORDED SUCCESSFULLY")
                    : ServiceResult.error(ack.getReturnCode(), ack.getMessage());
        }

        // 3. History must be passed
        if (!app.isHistoryPassed()) {
            return ServiceResult.error(ServiceResult.RC_HISTORY_NOT_PASSED,
                    "HISTORY CHECK MUST BE PASSED BEFORE PAYMENT");
        }

        // 4. Not already paid
        if (app.isPaid()) {
            return ServiceResult.error(ServiceResult.RC_ALREADY_PAID,
                    "PAYMENT HAS ALREADY BEEN RECORDED FOR THIS APPLICATION");
        }

        // 5. Look up fee
        Optional<LicenseFeeSchedule> feeOpt = feeDao.findActiveFee(app.getLicenseType(), "IF");
        if (feeOpt.isEmpty()) {
            return ServiceResult.error(ServiceResult.RC_FEE_NOT_FOUND,
                    "NO ACTIVE FEE SCHEDULE FOUND FOR THIS LICENSE TYPE");
        }
        BigDecimal feeAmount = feeOpt.get().getFeeAmount();

        // 6. Generate receipt number: RCP<yyyyMMdd><applicationId>
        String receiptNumber = "RCP"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + applicationId;

        // 7. Build payment record
        Payment payment = new Payment();
        payment.setApplicationId(applicationId);
        payment.setCandidateId(candidateId);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentAmount(feeAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentReference(paymentReference);
        payment.setPaymentStatus("S");
        payment.setLicenseType(app.getLicenseType());
        payment.setFeeType("IF");
        payment.setReceiptNumber(receiptNumber);
        payment.setProcessedBy(processedBy);

        // 8. Persist payment
        paymentDao.insert(payment);

        // 9. Advance the application in dlis-licensing-service
        ServiceResult<Void> ack = applicationGateway.paymentRecorded(applicationId, paymentReference, processedBy);
        if (!ack.isOk()) {
            // Business rejection by licensing: undo the local insert, keep the business error code
            sessionContext.setRollbackOnly();
            return ServiceResult.error(ack.getReturnCode(), ack.getMessage());
        }

        return ServiceResult.ok(payment, "PAYMENT RECORDED SUCCESSFULLY");
    }
}
