package com.dlis.payment.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/** JAX-RS Application for dlis-payment-service — same /api/v1 base path as the monolith. */
@ApplicationPath("/api/v1")
public class DlisApplication extends Application {
}
