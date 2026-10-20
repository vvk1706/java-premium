package com.dlis.core.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ServiceResult.
 */
class ServiceResultTest {

    // ── ok(payload) ───────────────────────────────────────────────────────────

    @Test
    void ok_withPayload_isOkAndDefaultMessage() {
        ServiceResult<String> result = ServiceResult.ok("data");

        assertTrue(result.isOk());
        assertFalse(result.isError());
        assertEquals(ServiceResult.RC_OK, result.getReturnCode());
        assertEquals("OK", result.getMessage());
        assertEquals("data", result.getPayload());
    }

    @Test
    void ok_nullPayload_isOkWithNullPayload() {
        ServiceResult<String> result = ServiceResult.ok(null);

        assertTrue(result.isOk());
        assertNull(result.getPayload());
        assertEquals("OK", result.getMessage());
    }

    // ── ok(payload, message) ──────────────────────────────────────────────────

    @Test
    void ok_withPayloadAndMessage_retainsBoth() {
        ServiceResult<Integer> result = ServiceResult.ok(42, "Created");

        assertTrue(result.isOk());
        assertFalse(result.isError());
        assertEquals(ServiceResult.RC_OK, result.getReturnCode());
        assertEquals("Created", result.getMessage());
        assertEquals(Integer.valueOf(42), result.getPayload());
    }

    // ── error ─────────────────────────────────────────────────────────────────

    @Test
    void error_withCodeAndMessage_isErrorWithNullPayload() {
        ServiceResult<String> result = ServiceResult.error(ServiceResult.RC_NOT_FOUND, "Not found");

        assertTrue(result.isError());
        assertFalse(result.isOk());
        assertEquals(ServiceResult.RC_NOT_FOUND, result.getReturnCode());
        assertEquals("Not found", result.getMessage());
        assertNull(result.getPayload());
    }

    @Test
    void error_withRcOkCode_isTreatedAsOk() {
        ServiceResult<String> result = ServiceResult.error(ServiceResult.RC_OK, "odd");

        assertTrue(result.isOk());
        assertFalse(result.isError());
    }

    @Test
    void error_withGenericErrorCode_isError() {
        ServiceResult<Object> result = ServiceResult.error(ServiceResult.RC_ERROR, "boom");

        assertTrue(result.isError());
        assertEquals(99, result.getReturnCode());
    }

    // ── return-code constants ─────────────────────────────────────────────────

    @Test
    void returnCodeConstants_haveExpectedValues() {
        assertEquals(0, ServiceResult.RC_OK);
        assertEquals(1, ServiceResult.RC_NOT_FOUND);
        assertEquals(1, ServiceResult.RC_DUPLICATE);
        assertEquals(2, ServiceResult.RC_INVALID_AGE);
        assertEquals(2, ServiceResult.RC_INVALID_TYPE);
        assertEquals(2, ServiceResult.RC_FEE_NOT_FOUND);
        assertEquals(3, ServiceResult.RC_ALREADY_EXISTS);
        assertEquals(3, ServiceResult.RC_ALREADY_PAID);
        assertEquals(4, ServiceResult.RC_HISTORY_NOT_PASSED);
        assertEquals(4, ServiceResult.RC_PAYMENT_NOT_DONE);
        assertEquals(3, ServiceResult.RC_NOT_AUTHORISED);
        assertEquals(5, ServiceResult.RC_INVALID_DECISION);
        assertEquals(99, ServiceResult.RC_ERROR);
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toString_ok_containsCodeMessageAndPayload() {
        String text = ServiceResult.ok("data", "Done").toString();

        assertEquals("ServiceResult{rc=0, msg='Done', payload=data}", text);
    }

    @Test
    void toString_error_containsCodeMessageAndNullPayload() {
        String text = ServiceResult.error(ServiceResult.RC_ERROR, "Failure").toString();

        assertEquals("ServiceResult{rc=99, msg='Failure', payload=null}", text);
    }

    // ── serialization ─────────────────────────────────────────────────────────

    @Test
    void serialization_roundTrip_preservesState() throws Exception {
        ServiceResult<String> original = ServiceResult.ok("data", "Done");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        Object copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = in.readObject();
        }

        assertTrue(copy instanceof ServiceResult);
        ServiceResult<?> restored = (ServiceResult<?>) copy;
        assertEquals(original.getReturnCode(), restored.getReturnCode());
        assertEquals(original.getMessage(), restored.getMessage());
        assertEquals(original.getPayload(), restored.getPayload());
    }
}
