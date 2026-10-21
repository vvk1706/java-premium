package com.dlis.candidate.rest;

import com.dlis.candidate.domain.Candidate;
import com.dlis.candidate.service.CandidateService;
import com.dlis.common.ApiErrors;
import com.dlis.common.ServiceResult;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;

/**
 * REST Resource: Candidate
 *
 * POST   /api/v1/candidates         — create candidate
 * GET    /api/v1/candidates/{id}    — get by primary key
 * GET    /api/v1/candidates?idNumber=... — find by national ID
 */
@Path("/candidates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CandidateResource {

    @Inject
    private CandidateService candidateService;

    @POST
    public Response createCandidate(CandidateRequest req) {
        if (req == null) {
            return ApiErrors.badRequest("Request body is required");
        }

        Candidate c = new Candidate();
        c.setFirstName(req.firstName);
        c.setLastName(req.lastName);
        c.setIdNumber(req.idNumber);
        c.setAddressLine1(req.addressLine1);
        c.setAddressLine2(req.addressLine2);
        c.setCity(req.city);
        c.setStateProvince(req.stateProvince);
        c.setPostalCode(req.postalCode);
        c.setCountry(req.country);
        c.setPhoneNumber(req.phoneNumber);
        c.setEmailAddress(req.emailAddress);

        try {
            c.setDateOfBirth(LocalDate.parse(req.dateOfBirth));
        } catch (Exception e) {
            return ApiErrors.badRequest("Invalid dateOfBirth format — use YYYY-MM-DD");
        }

        String userId = req.createdBy != null ? req.createdBy : "API";
        ServiceResult<Long> result = candidateService.createCandidate(c, userId);

        if (result.isOk()) {
            return Response.status(Response.Status.CREATED)
                    .entity(new IdResponse(result.getPayload(), result.getMessage()))
                    .build();
        }
        return ApiErrors.fromResult(result);
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        ServiceResult<Candidate> result = candidateService.findById(id);
        if (result.isOk()) {
            return Response.ok(result.getPayload()).build();
        }
        return ApiErrors.fromResult(result);
    }

    @GET
    public Response findByIdNumber(@QueryParam("idNumber") String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return ApiErrors.badRequest("idNumber query parameter is required");
        }
        ServiceResult<Candidate> result = candidateService.findByIdNumber(idNumber);
        if (result.isOk()) {
            return Response.ok(result.getPayload()).build();
        }
        return ApiErrors.fromResult(result);
    }

    // ── Request/Response DTOs ─────────────────────────────────────────────────

    public static class CandidateRequest {
        public String firstName;
        public String lastName;
        public String dateOfBirth;    // ISO-8601
        public String idNumber;
        public String addressLine1;
        public String addressLine2;
        public String city;
        public String stateProvince;
        public String postalCode;
        public String country;
        public String phoneNumber;
        public String emailAddress;
        public String createdBy;
    }

    public static class IdResponse {
        public Long   id;
        public String message;
        public IdResponse(Long id, String msg) { this.id = id; this.message = msg; }
    }
}
