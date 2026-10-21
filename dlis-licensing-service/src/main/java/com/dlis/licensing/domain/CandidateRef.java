package com.dlis.licensing.domain;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * The only facts the licensing workflow needs about a candidate. The candidate
 * record itself (PII) is owned by dlis-candidate-service.
 */
public class CandidateRef implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long      candidateId;
    private final LocalDate dateOfBirth;
    private final boolean   active;

    public CandidateRef(Long candidateId, LocalDate dateOfBirth, boolean active) {
        this.candidateId = candidateId;
        this.dateOfBirth = dateOfBirth;
        this.active      = active;
    }

    public Long      getCandidateId() { return candidateId; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public boolean   isActive()       { return active; }
}
