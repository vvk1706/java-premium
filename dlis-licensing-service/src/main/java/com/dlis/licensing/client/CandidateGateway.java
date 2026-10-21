package com.dlis.licensing.client;

import com.dlis.licensing.domain.CandidateRef;

import java.util.Optional;

/** Port to dlis-candidate-service (anti-corruption layer). */
public interface CandidateGateway {

    /** @return the candidate, or empty if the candidate service answers 404.
     *  @throws com.dlis.common.RemoteServiceException if the service cannot be reached. */
    Optional<CandidateRef> findById(Long candidateId);
}
