package com.dlis.licensing.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/** JAX-RS Application for dlis-licensing-service — same /api/v1 base path as the monolith. */
@ApplicationPath("/api/v1")
public class DlisApplication extends Application {
}
