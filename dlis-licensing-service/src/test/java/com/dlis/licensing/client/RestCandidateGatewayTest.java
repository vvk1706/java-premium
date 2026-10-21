package com.dlis.licensing.client;

import com.dlis.licensing.domain.CandidateRef;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RestCandidateGatewayTest {

    @Test
    void parse_mapsCandidateJsonFromCandidateService() {
        String json = "{\"candidateId\":42,\"firstName\":\"Jane\",\"dateOfBirth\":\"1990-05-15\","
                + "\"recordStatus\":\"A\",\"active\":true}";

        CandidateRef ref = RestCandidateGateway.parse(json);

        assertEquals(42L, ref.getCandidateId());
        assertEquals(LocalDate.of(1990, 5, 15), ref.getDateOfBirth());
        assertTrue(ref.isActive());
    }

    @Test
    void parse_inactiveAndMissingFlag() {
        CandidateRef ref = RestCandidateGateway.parse(
                "{\"candidateId\":1,\"dateOfBirth\":\"2000-01-01\"}");
        assertFalse(ref.isActive());
    }
}
