package com.dlis.payment.service;

import com.dlis.common.ServiceResult;
import com.dlis.payment.domain.Payment;

/**
 * Service interface for fee payment processing.
 * Derived from CA Gen action block AB-PROCESS-PAYMENT.
 */
public interface PaymentService {

    ServiceResult<Payment> processPayment(Long applicationId,
                                          Long candidateId,
                                          String paymentMethod,
                                          String paymentReference,
                                          String processedBy);
}
