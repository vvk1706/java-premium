# Program Architecture Documentation - DLIS

## 1. System Overview

The **Driver License Issuance System (DLIS)** is an enterprise Java modernization architecture hosted on **IBM zLinux** executing on **IBM WebSphere Application Server (WAS) Liberty**. It bridges modern HTTP REST / JSON interfaces with transactional **CICS Transaction Server** programs and an **IBM DB2** relational datastore.

```mermaid
graph TD
    User([REST Client / Web UI / Mobile]) -->|HTTP JSON| WAR[dlis-web.war]
    CICS([CICS Terminal / 3270 / COBOL]) -->|EBCDIC / COMMAREA| CICSBridge[dlis-cics-bridge.jar]
    
    subgraph "WAS Liberty JVM runtime (zLinux)"
        WAR -->|CDI / JNDI| EJBServices[dlis-core: Stateless EJBs]
        CICSBridge -->|CDI / JNDI| EJBServices
        
        subgraph "dlis-core.jar"
            EJBServices --> DAOs[JDBC DAOs]
            EJBServices --> Models[Domain Entities]
        end
    end
    
    DAOs -->|Type 4 JDBC: jdbc/dlisDS| DB2[(IBM DB2 on z/OS or LUW)]
```

---

## 2. Module Decomposition

### 2.1 `dlis-core` (Domain Model, Services, DAOs)
- **Packaging:** JAR
- **Purpose:** Encapsulates the core business models, stateless EJB service layer, and JDBC data access objects.
- **Key Packages:**
  - `com.dlis.core.domain`: Rich entity POJOs with domain validation helper methods ([`LicenseApplication`](dlis-core/src/main/java/com/dlis/core/domain/LicenseApplication.java:1), [`Candidate`](dlis-core/src/main/java/com/dlis/core/domain/Candidate.java:1), [`Payment`](dlis-core/src/main/java/com/dlis/core/domain/Payment.java:1), [`IssuedLicense`](dlis-core/src/main/java/com/dlis/core/domain/IssuedLicense.java:1), [`DrivingHistory`](dlis-core/src/main/java/com/dlis/core/domain/DrivingHistory.java:1), [`AuthorityUser`](dlis-core/src/main/java/com/dlis/core/domain/AuthorityUser.java:1), [`LicenseFeeSchedule`](dlis-core/src/main/java/com/dlis/core/domain/LicenseFeeSchedule.java:1)).
  - `com.dlis.core.service`: Service interfaces ([`ApplicationService`](dlis-core/src/main/java/com/dlis/core/service/ApplicationService.java:1), [`CandidateService`](dlis-core/src/main/java/com/dlis/core/service/CandidateService.java:1), [`PaymentService`](dlis-core/src/main/java/com/dlis/core/service/PaymentService.java:1), [`ApprovalService`](dlis-core/src/main/java/com/dlis/core/service/ApprovalService.java:1), [`ServiceResult`](dlis-core/src/main/java/com/dlis/core/service/ServiceResult.java:1)).
  - `com.dlis.core.service.ejb`: Enterprise Java Bean implementations annotated with `@Stateless` and `@TransactionAttribute(REQUIRED)`.
  - `com.dlis.core.dao` & `dao.jdbc`: Data access interfaces and implementations utilizing standard JDBC connected to `jdbc/dlisDS`.

### 2.2 `dlis-cics-bridge` (CICS JCICS Program Adapters)
- **Packaging:** JAR
- **Purpose:** Implements IBM JCICS `Executable` interfaces and binary byte-oriented COMMAREA serializers/deserializers for mainframe interoperability.
- **Key Components:**
  - `com.dlis.cics.commarea.BaseCommarea`: Abstract 108-byte header structure supporting standardized return codes, error messages, and transaction IDs.
  - `com.dlis.cics.commarea.ApplicationCommarea`: Extended 160-byte buffer for application inquiry and creation.
  - `com.dlis.cics.commarea.CandidateCommarea`: Extended 360-byte buffer for candidate registration.
  - `com.dlis.cics.program.DlisCandProgram`: CICS program `DLISCAND` (Transaction `CAND`).
  - `com.dlis.cics.program.DlisApplProgram`: CICS program `DLISAPPL` (Transaction `APPL`).
  - `com.dlis.cics.program.DlisPayApprProgram`: CICS program `DLISPAAP` (Transaction `PAAP`).

### 2.3 `dlis-web` (JAX-RS 2.1 REST API)
- **Packaging:** WAR
- **Context Root:** `/dlis/api/v1`
- **Key Components:**
  - `com.dlis.web.rest.DlisApplication`: JAX-RS application activator (`@ApplicationPath("/api/v1")`).
  - `com.dlis.web.rest.CandidateResource`: Endpoints for candidate onboarding and lookup.
  - `com.dlis.web.rest.ApplicationResource`: Endpoints for application lifecycle management and checks.
  - `com.dlis.web.rest.PaymentApprovalResource`: Endpoints for payments, approvals, and issuance.

### 2.4 `dlis-ear` (Enterprise Application Archive)
- **Packaging:** EAR
- **Liberty Configuration:** Configures JAX-RS 2.1, EJB Lite 3.2, CDI 2.0, JDBC 4.2 features, JNDI dataSource `jdbc/dlisDS`, and security roles.

---

## 3. Component Interaction Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Officer as Authority Officer
    participant API as PaymentApprovalResource (JAX-RS)
    participant EJB as ApprovalServiceEjb (EJB)
    participant DA as ApplicationDao
    participant DL as IssuedLicenseDao
    participant DB as IBM DB2 Database

    Officer->>API: POST /api/v1/applications/1001/issue
    API->>EJB: issueLicense(1001, "B", "None", "OFFICER1", "DEPT_TRANSPORT")
    activate EJB
    EJB->>DA: findById(1001)
    DA->>DB: SELECT * FROM DLIS.LICENSE_APPLICATION WHERE ID=1001
    DB-->>DA: Record (Status='AP', Elig='P', Hist='P', Pay='P')
    DA-->>EJB: Optional<LicenseApplication>
    
    Note over EJB: Calculate Expiry (5 yrs for 'O')<br/>Generate License Number: OPNB1021001<br/>Allocate Demerit Balance: 12
    
    EJB->>DL: insert(IssuedLicense)
    DL->>DB: INSERT INTO DLIS.ISSUED_LICENSE (...) VALUES (...)
    DB-->>DL: Generated Key (LICENSE_ID=501)
    
    EJB->>DA: updateApplicationStatus(1001, 'IS', now, "OFFICER1")
    DA->>DB: UPDATE DLIS.LICENSE_APPLICATION SET STATUS='IS' ...
    DB-->>DA: Row updated (1)
    
    EJB-->>API: ServiceResult.ok(IssuedLicense)
    deactivate EJB
    API-->>Officer: HTTP 201 Created (IssuedLicense JSON)
```
