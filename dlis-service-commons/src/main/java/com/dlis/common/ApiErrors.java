package com.dlis.common;

import javax.ws.rs.core.Response;

/**
 * Shared REST error mapping. Keeps the JSON error bodies and HTTP status codes
 * of the original monolith (404 for "not found", 422 for every other business error).
 */
public final class ApiErrors {

    private ApiErrors() {}

    public static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"" + message.replace("\"", "'") + "\"}").build();
    }

    public static <T> Response fromResult(ServiceResult<T> r) {
        int status = r.getReturnCode() == ServiceResult.RC_NOT_FOUND ? 404 : 422;
        return Response.status(status)
                .entity("{\"returnCode\":" + r.getReturnCode()
                        + ",\"message\":\"" + r.getMessage().replace("\"", "'") + "\"}")
                .build();
    }

    public static Response unavailable(String message) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity("{\"error\":\"" + message.replace("\"", "'") + "\"}").build();
    }
}
