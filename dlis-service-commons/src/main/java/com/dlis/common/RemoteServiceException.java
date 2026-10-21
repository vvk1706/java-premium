package com.dlis.common;

import javax.ejb.ApplicationException;

/**
 * A peer microservice could not be reached or answered with an unexpected status.
 * Marked as an EJB application exception so the container does not wrap it in an
 * EJBException, and so the current transaction is rolled back (no half-done saga step).
 */
@ApplicationException(rollback = true)
public class RemoteServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
