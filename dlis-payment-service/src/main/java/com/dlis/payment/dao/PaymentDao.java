package com.dlis.payment.dao;

import com.dlis.payment.domain.Payment;
import java.util.Optional;

/**
 * DAO interface for PAYMENT table.
 */
public interface PaymentDao {

    void insert(Payment payment);

    Optional<Payment> findById(Long paymentId);

    /** Idempotency lookup used when a saga step is retried. */
    Optional<Payment> findByApplicationAndReference(Long applicationId, String paymentReference);
}
