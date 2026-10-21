package com.dlis.licensing.rest;

import com.dlis.common.ApiErrors;
import com.dlis.common.ServiceResult;
import com.dlis.licensing.domain.IssuedLicense;
import com.dlis.licensing.service.ApprovalService;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Resource: Approval and issue workflow (payment moved to dlis-payment-service)
 *
 * POST /api/v1/applications/{id}/approval1    — first authority approval
 * POST /api/v1/applications/{id}/approval2    — second authority approval
 * POST /api/v1/applications/{id}/issue        — issue the license
 */
@Path("/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApprovalResource {

    @Inject private ApprovalService approvalService;

    @POST
    @Path("/{id}/approval1")
    public Response approval1(@PathParam("id") Long id, ApprovalRequest req) {
        if (req == null) return ApiErrors.badRequest("Request body required");
        ServiceResult<Void> result = approvalService.recordApproval1(
                id, req.authorityUserCode, req.decision, req.decisionNotes);
        return result.isOk()
                ? Response.ok(msg(result.getMessage())).build()
                : ApiErrors.fromResult(result);
    }

    @POST
    @Path("/{id}/approval2")
    public Response approval2(@PathParam("id") Long id, ApprovalRequest req) {
        if (req == null) return ApiErrors.badRequest("Request body required");
        ServiceResult<Void> result = approvalService.recordApproval2(
                id, req.authorityUserCode, req.decision, req.decisionNotes);
        return result.isOk()
                ? Response.ok(msg(result.getMessage())).build()
                : ApiErrors.fromResult(result);
    }

    @POST
    @Path("/{id}/issue")
    public Response issueLicense(@PathParam("id") Long id, IssueRequest req) {
        if (req == null) return ApiErrors.badRequest("Request body required");
        ServiceResult<IssuedLicense> result = approvalService.issueLicense(
                id, req.vehicleClass, req.restrictions,
                req.issuedByOfficer, req.issuedByAuthority);
        return result.isOk()
                ? Response.status(Response.Status.CREATED)
                    .entity(result.getPayload()).build()
                : ApiErrors.fromResult(result);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class ApprovalRequest {
        public String authorityUserCode;
        public String decision;       // A or R
        public String decisionNotes;
    }

    public static class IssueRequest {
        public String vehicleClass;   // A, B, C, D
        public String restrictions;
        public String issuedByOfficer;
        public String issuedByAuthority;
    }

    private Object msg(String m) {
        return "{\"message\":\"" + m.replace("\"","'") + "\"}";
    }
}
