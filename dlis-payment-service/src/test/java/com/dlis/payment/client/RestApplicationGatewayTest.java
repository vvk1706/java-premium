package com.dlis.payment.client;

import com.dlis.common.ServiceResult;
import com.dlis.payment.domain.ApplicationRef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RestApplicationGatewayTest {

    @Test
    void parse_readsLicensingApplicationJson() {
        ApplicationRef ref = RestApplicationGateway.parse(
                "{\"applicationId\":1000,\"candidateId\":7,\"licenseType\":\"P\","
                + "\"historyChkStatus\":\"P\",\"paymentStatus\":\"U\"}");

        assertEquals(1000L, ref.getApplicationId());
        assertEquals("P", ref.getLicenseType());
        assertTrue(ref.isHistoryPassed());
        assertFalse(ref.isPaid());
    }

    @Test
    void parse_paidAndHistoryNotPassed() {
        ApplicationRef ref = RestApplicationGateway.parse(
                "{\"applicationId\":1,\"licenseType\":\"L\",\"historyChkStatus\":\"F\",\"paymentStatus\":\"P\"}");

        assertFalse(ref.isHistoryPassed());
        assertTrue(ref.isPaid());
    }

    @Test
    void parseError_keepsLicensingReturnCodeAndMessage() {
        ServiceResult<Void> r = RestApplicationGateway.parseError(
                "{\"returnCode\":3,\"message\":\"ALREADY PAID\"}", 422);

        assertEquals(3, r.getReturnCode());
        assertEquals("ALREADY PAID", r.getMessage());
    }

    @Test
    void parseError_unparseableBodyFallsBackByStatus() {
        assertEquals(ServiceResult.RC_NOT_FOUND, RestApplicationGateway.parseError("<html>", 404).getReturnCode());
        assertEquals(ServiceResult.RC_ERROR, RestApplicationGateway.parseError("<html>", 422).getReturnCode());
    }
}
