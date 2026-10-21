package com.dlis.common;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/** Maps a failed downstream call to HTTP 503 so callers and the router can retry. */
@Provider
public class RemoteServiceExceptionMapper implements ExceptionMapper<RemoteServiceException> {

    @Override
    public Response toResponse(RemoteServiceException e) {
        return ApiErrors.unavailable(e.getMessage());
    }
}
