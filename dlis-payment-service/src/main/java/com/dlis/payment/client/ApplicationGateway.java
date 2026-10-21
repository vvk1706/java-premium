package com.dlis.payment.client;

import com.dlis.common.ServiceResult;
import com.dlis.payment.domain.ApplicationRef;

import java.util.Optional;

/** Port to dlis-licensing-service (anti-corruption layer). */
public interface ApplicationGateway {

    /** @return the application, or empty on HTTP 404.
     *  @throws com.dlis.common.RemoteServiceException when licensing cannot be reached. */
    Optional<ApplicationRef> findById(Long applicationId);

    /** Saga step: tell licensing the payment exists. Idempotent per paymentReference on the licensing side. */
    ServiceResult<Void> paymentRecorded(Long applicationId, String paymentReference, String recordedBy);
}
