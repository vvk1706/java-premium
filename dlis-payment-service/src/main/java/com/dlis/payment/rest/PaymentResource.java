package com.dlis.payment.rest;

import com.dlis.common.ApiErrors;
import com.dlis.common.ServiceResult;
import com.dlis.payment.domain.Payment;
import com.dlis.payment.service.PaymentService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Resource: Payment
 *
 * POST /api/v1/applications/{id}/payment — process payment (URL unchanged from the monolith;
 * the OpenShift Route sends this path to dlis-payment-service).
 */
@Path("/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject private PaymentService paymentService;

    @POST
    @Path("/{id}/payment")
    public Response processPayment(@PathParam("id") Long id, PaymentRequest req) {
        if (req == null) return ApiErrors.badRequest("Request body required");
        ServiceResult<Payment> result = paymentService.processPayment(
                id, req.candidateId, req.paymentMethod, req.paymentReference,
                nullOrDefault(req.processedBy, "API"));
        if (result.isOk()) {
            return Response.ok(new PaymentResponse(result.getPayload(), result.getMessage())).build();
        }
        return ApiErrors.fromResult(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class PaymentRequest {
        public Long   candidateId;
        public String paymentMethod;
        public String paymentReference;
        public String processedBy;
    }

    public static class PaymentResponse {
        public Long   paymentId;
        public String receiptNumber;
        public String message;
        public PaymentResponse(Payment p, String m) {
            this.paymentId = p != null ? p.getPaymentId() : null;
            this.receiptNumber = p != null ? p.getReceiptNumber() : null;
            this.message = m;
        }
    }

    private static String nullOrDefault(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
