package com.dlis.licensing.rest;

import com.dlis.common.ApiErrors;
import com.dlis.common.ServiceResult;
import com.dlis.licensing.domain.LicenseApplication;
import com.dlis.licensing.service.ApplicationService;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Resource: License Applications
 *
 * POST /api/v1/applications                            — create application
 * GET  /api/v1/applications/{id}                      — inquire status
 * POST /api/v1/applications/{id}/eligibility-check    — run eligibility check
 * POST /api/v1/applications/{id}/history-check        — run history check
 * POST /api/v1/applications/{id}/payment-recorded     — INTERNAL: callback from dlis-payment-service
 */
@Path("/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApplicationResource {

    @Inject
    private ApplicationService applicationService;

    @POST
    public Response createApplication(CreateApplicationRequest req) {
        if (req == null) {
            return ApiErrors.badRequest("Request body required");
        }
        String userId = req.createdBy != null ? req.createdBy : "API";
        ServiceResult<Long> result =
                applicationService.createApplication(req.candidateId, req.licenseType, userId);
        if (result.isOk()) {
            return Response.status(Response.Status.CREATED)
                    .entity(new IdResponse(result.getPayload(), result.getMessage()))
                    .build();
        }
        return ApiErrors.fromResult(result);
    }

    @GET
    @Path("/{id}")
    public Response inquireStatus(@PathParam("id") Long id) {
        ServiceResult<LicenseApplication> result = applicationService.inquireStatus(id);
        if (result.isOk()) {
            return Response.ok(result.getPayload()).build();
        }
        return ApiErrors.fromResult(result);
    }

    @POST
    @Path("/{id}/eligibility-check")
    public Response eligibilityCheck(@PathParam("id") Long id,
                                     CheckRequest req) {
        String userId = req != null && req.checkedBy != null ? req.checkedBy : "API";
        ServiceResult<String> result = applicationService.checkEligibility(id, userId);
        return result.isOk()
                ? Response.ok(new StatusResponse(result.getPayload(), result.getMessage())).build()
                : ApiErrors.fromResult(result);
    }

    @POST
    @Path("/{id}/history-check")
    public Response historyCheck(@PathParam("id") Long id,
                                 CheckRequest req) {
        String userId = req != null && req.checkedBy != null ? req.checkedBy : "API";
        ServiceResult<String> result = applicationService.checkHistory(id, userId);
        return result.isOk()
                ? Response.ok(new StatusResponse(result.getPayload(), result.getMessage())).build()
                : ApiErrors.fromResult(result);
    }

    /** Internal saga callback; only the in-cluster DLIS_SERVICE account may call it (also blocked at the gateway). */
    @POST
    @Path("/{id}/payment-recorded")
    @RolesAllowed("DLIS_SERVICE")
    public Response paymentRecorded(@PathParam("id") Long id, PaymentRecordedRequest req) {
        if (req == null || req.paymentReference == null || req.paymentReference.isBlank()) {
            return ApiErrors.badRequest("paymentReference is required");
        }
        String by = req.recordedBy != null ? req.recordedBy : "API";
        ServiceResult<Void> result = applicationService.recordPayment(id, req.paymentReference, by);
        return result.isOk()
                ? Response.ok("{\"message\":\"" + result.getMessage() + "\"}").build()
                : ApiErrors.fromResult(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class CreateApplicationRequest {
        public Long   candidateId;
        public String licenseType;
        public String createdBy;
    }

    public static class PaymentRecordedRequest {
        public String paymentReference;
        public String recordedBy;
    }

    public static class CheckRequest {
        public String checkedBy;
    }

    public static class IdResponse {
        public Long id; public String message;
        public IdResponse(Long id, String m) { this.id = id; this.message = m; }
    }

    public static class StatusResponse {
        public String status; public String message;
        public StatusResponse(String s, String m) { this.status = s; this.message = m; }
    }
}
