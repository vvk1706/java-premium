package com.dlis.licensing.dao;

import com.dlis.licensing.domain.DrivingHistory;
import java.util.List;

/**
 * DAO interface for DLIS.DRIVING_HISTORY table.
 */
public interface DrivingHistoryDao {

    /** Returns all active records for a candidate. */
    List<DrivingHistory> findActiveByCandidateId(Long candidateId);
}
