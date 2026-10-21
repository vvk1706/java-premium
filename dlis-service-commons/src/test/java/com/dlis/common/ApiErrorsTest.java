package com.dlis.common;

import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiErrorsTest {

    @Test
    void fromResult_notFoundIs404() {
        Response r = ApiErrors.fromResult(ServiceResult.error(ServiceResult.RC_NOT_FOUND, "NOPE"));
        assertEquals(404, r.getStatus());
        assertEquals("{\"returnCode\":1,\"message\":\"NOPE\"}", r.getEntity());
    }

    @Test
    void fromResult_otherErrorsAre422AndQuotesAreEscaped() {
        Response r = ApiErrors.fromResult(ServiceResult.error(3, "say \"hi\""));
        assertEquals(422, r.getStatus());
        assertEquals("{\"returnCode\":3,\"message\":\"say 'hi'\"}", r.getEntity());
    }

    @Test
    void badRequestAndUnavailable() {
        assertEquals(400, ApiErrors.badRequest("x").getStatus());
        assertEquals(503, ApiErrors.unavailable("x").getStatus());
    }
}
