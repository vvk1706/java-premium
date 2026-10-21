package com.dlis.common;

import org.junit.jupiter.api.Test;

import javax.ejb.ApplicationException;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class RemoteServiceExceptionMapperTest {

    @Test
    void mapsTo503WithJsonBody() {
        RuntimeException cause = new RuntimeException("boom");
        Response r = new RemoteServiceExceptionMapper()
                .toResponse(new RemoteServiceException("candidate-service unavailable", cause));
        assertEquals(503, r.getStatus());
        assertEquals("{\"error\":\"candidate-service unavailable\"}", r.getEntity());
    }

    @Test
    void isRollbackApplicationException() {
        ApplicationException ae = RemoteServiceException.class.getAnnotation(ApplicationException.class);
        assertNotNull(ae);
        assertTrue(ae.rollback());
    }
}
