package com.dlis.payment.domain;

import java.io.Serializable;

/** The facts payment needs about an application. The application itself is owned by dlis-licensing-service. */
public class ApplicationRef implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long    applicationId;
    private final String  licenseType;
    private final boolean historyPassed;
    private final boolean paid;

    public ApplicationRef(Long applicationId, String licenseType, boolean historyPassed, boolean paid) {
        this.applicationId = applicationId;
        this.licenseType   = licenseType;
        this.historyPassed = historyPassed;
        this.paid          = paid;
    }

    public Long    getApplicationId() { return applicationId; }
    public String  getLicenseType()   { return licenseType; }
    public boolean isHistoryPassed()  { return historyPassed; }
    public boolean isPaid()           { return paid; }
}
