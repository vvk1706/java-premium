# Driver License Issuance System (DLIS)

DLIS is a modernized enterprise Java application running on **IBM zLinux** under **IBM WebSphere Application Server (WAS) Liberty 23.x**, backed by **IBM DB2** and integrated with **CICS Transaction Server**.

---

## 🏛️ Architecture & Modules

The system is organized as a multi-module Maven project:

```
dlis-parent/
├── dlis-core/          # Stateless EJBs, JDBC DAOs, Domain entities
├── dlis-cics-bridge/   # IBM JCICS programs and COMMAREA adapters
├── dlis-web/           # JAX-RS 2.1 REST API endpoints
└── dlis-ear/           # WebSphere Liberty EAR assembly
```

---

## 🚀 Key Features & Capabilities

- **Candidate Management:** Secure applicant onboarding and duplicate identity validation.
- **Sequential Application Pipeline:** Strict validation pipeline (`PE` $\rightarrow$ `EC` $\rightarrow$ `HC` $\rightarrow$ `PA` $\rightarrow$ `A2` $\rightarrow$ `AP` $\rightarrow$ `IS`).
- **Eligibility & Upgrade Validation:** Age threshold validation (Learner $\ge 16$, Probation $\ge 17$, Open $\ge 18$) and progression prerequisites.
- **Driving History & Demerit Point Cap:** Verification against suspensions, disqualifications, unpaid fines, and 12-point demerit thresholds.
- **Dual-Level Authority Approval:** Segregation of duties between Level 1 officers and Level 2 supervisors.
- **Dual Access Channels:** Simultaneous RESTful web services (`/dlis/api/v1`) and CICS transactions (`CAND`, `APPL`, `PAAP`).

---

## 📚 Documentation & Wiki

Comprehensive system documentation is generated in the [`docs/`](docs/) directory:

- **Interactive Wiki:** Open [`docs/index.html`](docs/index.html) in any browser.
- **Business Rules:** [`docs/business/business-rules.md`](docs/business/business-rules.md) and [`docs/business/decision-matrix.md`](docs/business/decision-matrix.md).
- **Program Architecture:** [`docs/program/architecture.md`](docs/program/architecture.md).
- **REST API Reference:** [`docs/program/api-reference.md`](docs/program/api-reference.md).
- **CICS Bridge & COMMAREA:** [`docs/program/cics-commarea.md`](docs/program/cics-commarea.md).
- **Database Schema:** [`docs/program/database-schema.md`](docs/program/database-schema.md).

---

## 🛠️ Build & Test

```bash
# Build all modules and run unit tests
mvn clean test

# Package EAR for Liberty deployment
mvn package
```
