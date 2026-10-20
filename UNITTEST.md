# DLIS Unit Testing Strategy

Unit testing strategy for the **Driver License Issuance System (DLIS)** — a multi-module Maven project (Java 11) that runs on IBM WebSphere Liberty 23.x with DB2 and CICS integration.

---

## 1. Project Architecture and Key Components

### 1.1 Module overview

| Module | Packaging | Responsibility | Test priority |
|---|---|---|---|
| [`dlis-core`](dlis-core) | jar | Domain model, service interfaces, stateless EJB implementations, JDBC DAOs | **High** – contains nearly all business logic |
| [`dlis-cics-bridge`](dlis-cics-bridge) | jar | JCICS programs (`Executable`) and COMMAREA byte-level adapters | **High** – binary layout/encoding must be exact |
| [`dlis-web`](dlis-web) | war | JAX-RS 2.1 REST resources (`/dlis/api/v1`) | **Medium** – thin layer, mapping of `ServiceResult` to HTTP |
| [`dlis-ear`](dlis-ear) | ear | EAR assembly, Liberty `server.xml`, JaCoCo aggregate report | **None** (no code); used only for aggregated coverage |

Dependency direction: `dlis-web` → `dlis-cics-bridge` → `dlis-core`; `dlis-ear` packages all.

### 1.2 Key components requiring tests

**`dlis-core`** (`com.dlis.core`)

| Layer | Classes | Notes |
|---|---|---|
| Result type | [`ServiceResult`](dlis-core/src/main/java/com/dlis/core/service/ServiceResult.java) | Factories `ok(...)`/`error(...)`, `isOk()`/`isError()`, return-code constants |
| Services (EJB) | [`CandidateServiceEjb`](dlis-core/src/main/java/com/dlis/core/service/ejb/CandidateServiceEjb.java), [`ApplicationServiceEjb`](dlis-core/src/main/java/com/dlis/core/service/ejb/ApplicationServiceEjb.java), [`PaymentServiceEjb`](dlis-core/src/main/java/com/dlis/core/service/ejb/PaymentServiceEjb.java), [`ApprovalServiceEjb`](dlis-core/src/main/java/com/dlis/core/service/ejb/ApprovalServiceEjb.java) | Business rules (pipeline `PE→EC→HC→PA→A2→AP→IS`, age thresholds, 12-point demerit cap, dual approval). DAOs injected with `@Inject`; `PaymentServiceEjb` and `ApprovalServiceEjb` additionally use an `@Resource` `DataSource` directly |
| DAOs (JDBC) | [`CandidateDaoImpl`](dlis-core/src/main/java/com/dlis/core/dao/jdbc/CandidateDaoImpl.java), [`ApplicationDaoImpl`](dlis-core/src/main/java/com/dlis/core/dao/jdbc/ApplicationDaoImpl.java), `DrivingHistoryDaoImpl` / `IssuedLicenseDaoImpl` / `LicenseFeeScheduleDaoImpl` in [`SupportingDaoImpls`](dlis-core/src/main/java/com/dlis/core/dao/jdbc/SupportingDaoImpls.java) | Raw JDBC, `@Resource(lookup="jdbc/dlisDS")` DataSource, row mappers, `java.sql.Date` ↔ `LocalDate` conversion |
| Domain | `Candidate`, `LicenseApplication`, `Payment`, `DrivingHistory`, `IssuedLicense`, `LicenseFeeSchedule`, `AuthorityUser` | Mostly POJOs; test only those with logic (e.g. `LicenseApplication.isHistoryPassed()`, status constants) |

**`dlis-cics-bridge`** (`com.dlis.cics`)

| Layer | Classes | Notes |
|---|---|---|
| COMMAREA | [`BaseCommarea`](dlis-cics-bridge/src/main/java/com/dlis/cics/commarea/BaseCommarea.java), `CandidateCommarea`, `ApplicationCommarea` | `toBytes()`/`fromBytes()`, fixed-width `encodeString`/`decodeString`/`encodeNumeric`/`decodeNumeric`, `MAX_SIZE = 32767`, EBCDIC-space (`0x40`) padding |
| CICS programs | `DlisCandProgram`, `DlisApplProgram`, `DlisPayApprProgram` | Implement `com.ibm.cics.server.Executable`; `execute(CommAreaHolder)` deserializes COMMAREA, calls injected services, serializes result |

**`dlis-web`** (`com.dlis.web.rest`)

| Classes | Notes |
|---|---|
| [`CandidateResource`](dlis-web/src/main/java/com/dlis/web/rest/CandidateResource.java), [`ApplicationResource`](dlis-web/src/main/java/com/dlis/web/rest/ApplicationResource.java), [`PaymentApprovalResource`](dlis-web/src/main/java/com/dlis/web/rest/PaymentApprovalResource.java) | Inject services, map `ServiceResult` → `javax.ws.rs.core.Response` (status codes, entity DTOs) |
| `DlisApplication` | JAX-RS `Application` subclass; trivial |

### 1.3 Current test state

| Module | Existing tests |
|---|---|
| `dlis-core` | [`CandidateServiceEjbTest`](dlis-core/src/test/java/com/dlis/core/service/ejb/CandidateServiceEjbTest.java) (6 tests), [`ApplicationServiceEjbTest`](dlis-core/src/test/java/com/dlis/core/service/ejb/ApplicationServiceEjbTest.java) (9 tests) |
| `dlis-cics-bridge` | none |
| `dlis-web` | none |

**Gaps:** `PaymentServiceEjb`, `ApprovalServiceEjb`, `ApplicationServiceEjb.inquireStatus`, all JDBC DAOs, `ServiceResult`, all COMMAREAs, all CICS programs, all REST resources.

---

## 2. Unit Testing Approach per Module / Component

General principles: tests are **pure unit tests** – no Liberty, no DB2, no CICS region. All collaborators are mocked with Mockito. The existing style (JUnit 5 + `MockitoExtension` + `@Mock`/`@InjectMocks`, AAA structure, descriptive `method_scenario[_expected]` names) is the project standard and must be followed.

### 2.1 `dlis-core`

#### ServiceResult
- `ok(payload)` / `ok(payload, message)` → `isOk()==true`, `isError()==false`, payload/message retained.
- `error(code, message)` → `isError()==true`, `getPayload()==null`.
- `toString()` contains code/message.

#### Service EJBs (mock DAOs, field injection via `@InjectMocks`)
| Class | Scenarios to cover |
|---|---|
| `CandidateServiceEjb` (extend existing) | Success; duplicate ID number; future/null DOB; `findByIdNumber` found / not found; `findById` found / not found; boundary DOB = today |
| `ApplicationServiceEjb` (extend existing) | `createApplication`: already-active application (`RC_ALREADY_EXISTS`), each valid license type (`L`,`P`,`O`). `checkEligibility`: age boundaries at 16 / 17 / 18 (exactly-on and one-day-short), Probation requires active Learner, Open requires active Probation, application not found, already checked. `checkHistory`: suspension/disqualification, exactly 12 vs 13 demerit points, fine paid vs unpaid, multiple records summed, null `fineAmount`. `inquireStatus` found / not found |
| `PaymentServiceEjb` | Application not found; history not passed (`RC_HISTORY_NOT_PASSED`); already paid (`RC_ALREADY_PAID`); no fee schedule (`RC_FEE_NOT_FOUND`); success (receipt number format `RCP<yyyyMMdd><applicationId>`, `updatePayment` invoked with `STATUS_PAYMENT_APPROVED`). The inline JDBC `insertPayment` requires a **mocked `DataSource`/`Connection`/`PreparedStatement`/`ResultSet`** injected via `ReflectionTestUtils`-style reflection (`Field.setAccessible`) or `@InjectMocks` with `@Mock DataSource`; also test `SQLException` → `RuntimeException` |
| `ApprovalServiceEjb` | Level‑1 and Level‑2 approval happy path; payment not done (`RC_PAYMENT_NOT_DONE`); unauthorised user / wrong level (`RC_NOT_AUTHORISED`); invalid decision (`RC_INVALID_DECISION`); segregation of duties (Level 1 ≠ Level 2 user); `issueLicense` requires both approvals; `findAuthority` via mocked `DataSource` |

Use `verify(..., never())` for side effects that must **not** occur on failure paths, and `ArgumentCaptor` to assert persisted values (e.g. `Payment` fields).

#### JDBC DAOs (`*DaoImpl`)
Unit tests with mocked JDBC (`DataSource` → `Connection` → `PreparedStatement` → `ResultSet`):
- Verify SQL parameter binding order and types (`setLong`, `setDate`, `setBigDecimal`, `setString`).
- Verify row mapping (`ResultSet` → domain object), including `null` dates (`toLD`) and empty results (`Optional.empty()`).
- Verify generated-key handling in `insert`.
- Verify `SQLException` is translated as the implementation specifies.
- **Out of scope for unit tests:** executing SQL against DB2 (belongs in integration tests using the scripts in `dlis-core/src/main/resources/db2/`).

#### Domain
- Test only methods with logic (status helpers such as `isHistoryPassed()`), not plain getters/setters.

### 2.2 `dlis-cics-bridge`

#### COMMAREA classes (pure Java – no mocks needed)
- **Round-trip:** `fromBytes(toBytes())` returns equal field values for `CandidateCommarea` and `ApplicationCommarea`.
- **Length:** `toBytes().length` equals the documented COMMAREA length ([`docs/program/cics-commarea.md`](docs/program/cics-commarea.md)) and never exceeds `MAX_SIZE` (32767).
- **Encoding helpers** (`encodeString`, `decodeString`, `encodeNumeric`, `decodeNumeric`): padding with `0x40`, truncation of over-long values, `null` input, zero-padding of numerics, empty/blank numeric → `0`, leading/trailing blank trimming. Because they are `protected static`, test through a small test subclass in package `com.dlis.cics.commarea` or via public `toBytes()/fromBytes()`.
- **Fixed offsets:** assert specific byte ranges (header: tran code, return code, message, user id) against the COMMAREA spec.
- **Negative:** `fromBytes` with too-short/`null` array behaves as implemented (document and lock down current behaviour).

#### CICS programs (`Dlis*Program`)
- `com.ibm.cics.server` is `provided` scope; classes are on the test classpath. Mock `CommAreaHolder` (and any JCICS static calls) with Mockito; Mockito 5 uses the inline mock maker by default, so final classes can be mocked, and `mockStatic` is available if needed (add `mockito-inline` is **not** required for 5.x).
- Inject mocked services via `@InjectMocks`.
- Assert: valid COMMAREA → service called with mapped arguments → return code/message written back to COMMAREA bytes; unknown transaction code; service error → non-zero `returnCode` and message; exception handling path.
- Do **not** attempt to run a real CICS region in unit tests.

### 2.3 `dlis-web`

- Instantiate resources directly (POJO) with `@Mock` services and `@InjectMocks`; no JAX-RS container required. `javax.ws.rs.core.Response` implementation (`RuntimeDelegate`) is not available from the `jakarta.jakartaee-api` API jar alone – add a lightweight provider **only if** `Response.status(...)` fails at runtime in tests (see §5, optional dependencies).
- For each endpoint verify: HTTP status mapping (201/200/400/404/409/422 etc. as implemented), entity DTO content (`IdResponse`, `StatusResponse`, `PaymentResponse`), correct delegation (`verify(service).method(args)`), and request → domain mapping (e.g. `CandidateRequest` → `Candidate`).
- Cover `findByIdNumber` with `null`/blank query param.

### 2.4 `dlis-ear`
No unit tests. The module only produces the aggregate JaCoCo report (`report-aggregate` at `verify`).

---

## 3. Test File Paths and Naming Conventions

Follow standard Maven layout and the conventions already used in `dlis-core`:

| Rule | Convention | Example |
|---|---|---|
| Location | `<module>/src/test/java/<same package as class under test>` | `dlis-core/src/test/java/com/dlis/core/service/ejb/` |
| Test class name | `<ClassUnderTest>Test` | `PaymentServiceEjbTest` |
| Test method name | `<method>_<scenario>[_<expectedResult>]` | `processPayment_alreadyPaid_returnsError` |
| Visibility | package-private class and methods | `class PaymentServiceEjbTest { @Test void ... }` |
| Section separators | `// ── methodName ──` comments between groups (as in existing tests) | |
| Test resources | `<module>/src/test/resources/` | `commarea/candidate-sample.bin` |
| Surefire pattern | `*Test.java` (default; also `Test*`, `*Tests`, `*TestCase`) | |

Planned test files:

```
dlis-core/src/test/java/com/dlis/core/
├── service/
│   ├── ServiceResultTest.java
│   └── ejb/
│       ├── CandidateServiceEjbTest.java        (existing)
│       ├── ApplicationServiceEjbTest.java      (existing)
│       ├── PaymentServiceEjbTest.java
│       └── ApprovalServiceEjbTest.java
├── dao/jdbc/
│   ├── CandidateDaoImplTest.java
│   ├── ApplicationDaoImplTest.java
│   ├── DrivingHistoryDaoImplTest.java
│   ├── IssuedLicenseDaoImplTest.java
│   └── LicenseFeeScheduleDaoImplTest.java
└── domain/
    └── LicenseApplicationTest.java

dlis-cics-bridge/src/test/java/com/dlis/cics/
├── commarea/
│   ├── BaseCommareaTest.java
│   ├── CandidateCommareaTest.java
│   └── ApplicationCommareaTest.java
└── program/
    ├── DlisCandProgramTest.java
    ├── DlisApplProgramTest.java
    └── DlisPayApprProgramTest.java

dlis-web/src/test/java/com/dlis/web/rest/
├── CandidateResourceTest.java
├── ApplicationResourceTest.java
└── PaymentApprovalResourceTest.java
```

> `SupportingDaoImpls.java` holds three package-private/secondary classes; create one test class per DAO impl in the `com.dlis.core.dao.jdbc` package.

---

## 4. Test Commands

Build tool: **Maven**. Run from the repository root (`/Users/valerykornienko/workspace/java-premium`).

### 4.1 Without coverage

```bash
# All modules
mvn test

# Clean build + all tests
mvn clean test

# Single module (with its dependencies built first)
mvn test -pl dlis-core -am
mvn test -pl dlis-cics-bridge -am
mvn test -pl dlis-web -am

# Single test class
mvn test -pl dlis-core -Dtest=PaymentServiceEjbTest

# Single test method
mvn test -pl dlis-core -Dtest=PaymentServiceEjbTest#processPayment_alreadyPaid_returnsError

# Several classes / pattern
mvn test -Dtest='CandidateServiceEjbTest,ApplicationServiceEjbTest'
mvn test -Dtest='*ServiceEjbTest' -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

When running with `-Dtest` across multiple modules, add `-Dsurefire.failIfNoSpecifiedTests=false` so modules that do not contain the class do not fail.

### 4.2 With JaCoCo coverage

JaCoCo `${jacoco.version}` (0.8.15) is configured in the parent [`pom.xml`](pom.xml) `pluginManagement` (`prepare-agent` + `report` bound to the `test` phase) and activated in `dlis-core`, `dlis-cics-bridge`, `dlis-web` and `dlis-ear`.

```bash
# Tests + per-module coverage report
mvn test jacoco:report

# Clean + tests + report (recommended)
mvn clean test jacoco:report

# Single module with coverage
mvn clean test jacoco:report -pl dlis-core -am

# Cross-module aggregate report (generated by dlis-ear at the verify phase)
mvn clean verify
```

### 4.3 Report locations

| Report | Path |
|---|---|
| Per-module HTML | `<module>/target/site/jacoco/index.html` (e.g. `dlis-core/target/site/jacoco/index.html`) |
| Per-module CSV | `<module>/target/site/jacoco/jacoco.csv` |
| Per-module XML | `<module>/target/site/jacoco/jacoco.xml` |
| Aggregate HTML | `dlis-ear/target/site/jacoco-aggregate/index.html` |
| Surefire results | `<module>/target/surefire-reports/` |
| Raw execution data | `<module>/target/jacoco.exec` |

### 4.4 Build notes

- Java 11 is required (`maven.compiler.source/target = 11`); run with JDK 11+.
- `jakartaee-api`, `com.ibm.cics.server` and `jcc` are `provided` scope and are resolved from the IBM Maven repository; they are available on the test classpath.
- **Surefire version:** only `dlis-core` pins `maven-surefire-plugin` `3.1.2`. `dlis-cics-bridge` and `dlis-web` currently inherit Maven's default Surefire version, which may be too old to discover JUnit 5 tests. **Before adding tests to those modules, move the Surefire `3.1.2` declaration into the parent `pluginManagement` / `<build><plugins>`** (or add it to the child POMs), otherwise their tests will be silently skipped.
- The `jacoco:report` goal needs the `jacoco.exec` produced by `prepare-agent`; always run it in the same invocation as `test` (as above).

---

## 5. Recommended Frameworks and Libraries

### 5.1 Already configured

| Purpose | Library | Version property | Coordinates |
|---|---|---|---|
| Test framework | **JUnit 5 (Jupiter)** | `${junit.version}` = 5.10.0 | `org.junit.jupiter:junit-jupiter` |
| Mocking | **Mockito** | `${mockito.version}` = 5.5.0 | `org.mockito:mockito-core` |
| Coverage | **JaCoCo** | `${jacoco.version}` = 0.8.15 | `org.jacoco:jacoco-maven-plugin` |
| Test runner | Maven Surefire | 3.1.2 (core only) | `org.apache.maven.plugins:maven-surefire-plugin` |

> **Action required:** `MockitoExtension` lives in `org.mockito:mockito-junit-jupiter`, which is **not** a transitive dependency of `mockito-core`. The existing tests import `org.mockito.junit.jupiter.MockitoExtension`, but no module POM declares this artifact. Add `org.mockito:mockito-junit-jupiter:${mockito.version}` (test scope) to the parent `dependencyManagement` and to each module's dependencies before running the tests.

### 5.2 Optional, add only when needed

| Need | Library | When |
|---|---|---|
| Fluent assertions | AssertJ (`org.assertj:assertj-core`) | If assertion readability on collections/exceptions becomes an issue; otherwise stay with JUnit `Assertions` for consistency |
| JAX-RS `Response` runtime in unit tests | `org.glassfish.jersey.core:jersey-common` (or `org.apache.cxf:cxf-rt-frontend-jaxrs`) at `test` scope | Only if `Response.status(...).build()` throws `ClassNotFoundException`/`RuntimeDelegate` errors in `dlis-web` tests |
| Parametrised boundary tests | `junit-jupiter-params` (included in the `junit-jupiter` aggregate) | Age thresholds, license-type matrix, COMMAREA field lengths |
| In-memory DB for DAO integration tests | H2 in DB2 compatibility mode (`MODE=DB2`) | Future integration-test phase – **not** unit tests |
| Time control | `java.time.Clock` injection or Mockito `mockStatic(LocalDate.class)` | Only if date-dependent logic cannot be made deterministic via test data (current tests use `LocalDate.now().minusYears(n)`) |

### 5.3 Conventions
- Use `@ExtendWith(MockitoExtension.class)` (strict stubs): remove unused stubbings; do not use `lenient()` unless justified.
- One logical behaviour per test; AAA (Arrange / Act / Assert) with blank-line separation.
- Use `@ParameterizedTest` + `@CsvSource` for boundary matrices instead of copy-pasted tests.
- Avoid real time dependence at boundaries by deriving dates from `LocalDate.now()` consistently (as the existing tests do) and avoid asserting around midnight rollover.
- No network, filesystem, DB or CICS access in unit tests; tests must run in < 1s each.

---

## 6. Quality Metrics and Coverage Thresholds

### 6.1 Coverage targets (JaCoCo)

| Scope | Line | Branch | Method |
|---|---|---|---|
| **Service EJBs** (`service.ejb`) | ≥ 90% | ≥ 85% | ≥ 95% |
| `ServiceResult` | 100% | 100% | 100% |
| **COMMAREA classes** | ≥ 95% | ≥ 90% | 100% |
| **CICS programs** | ≥ 80% | ≥ 75% | ≥ 90% |
| **JDBC DAOs** | ≥ 80% | ≥ 70% | ≥ 90% |
| **REST resources** | ≥ 85% | ≥ 80% | ≥ 90% |
| Domain POJOs | Not enforced (getters/setters) | – | – |
| **Overall project (aggregate)** | **≥ 80%** | **≥ 70%** | ≥ 85% |

Recommended phased rollout: **Phase 1** – `dlis-core` services ≥ 80% line; **Phase 2** – COMMAREA + REST; **Phase 3** – DAOs + CICS programs; then tighten to the targets above.

### 6.2 Enforcing thresholds (optional – add to parent `pom.xml`)

```xml
<execution>
  <id>jacoco-check</id>
  <phase>verify</phase>
  <goals><goal>check</goal></goals>
  <configuration>
    <rules>
      <rule>
        <element>BUNDLE</element>
        <limits>
          <limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.80</minimum></limit>
          <limit><counter>BRANCH</counter><value>COVEREDRATIO</value><minimum>0.70</minimum></limit>
        </limits>
      </rule>
    </rules>
  </configuration>
</execution>
```

Run with `mvn clean verify`. Introduce the `check` goal only once the baseline is reached to avoid breaking the build.

### 6.3 Quality indicators beyond coverage

| Metric | Target |
|---|---|
| Test pass rate | 100% on `main`; no `@Disabled` without a linked issue |
| Business-rule traceability | Every rule in [`docs/business/business-rules.md`](docs/business/business-rules.md) and every row of [`docs/business/decision-matrix.md`](docs/business/decision-matrix.md) maps to ≥ 1 test (positive and negative) |
| Every `ServiceResult.RC_*` code | Produced and asserted by ≥ 1 test |
| Every REST endpoint | ≥ 1 success + ≥ 1 failure test, status code asserted |
| Boundary coverage | Age 15/16/17/18, demerit 12/13, empty/null inputs tested |
| Test speed | Whole unit suite < 30 s; single test < 1 s |
| Flakiness | 0 flaky tests; no time/ordering/random dependence |
| Strict mocking | No `UnnecessaryStubbingException`; verify `never()` on prohibited side effects |
| Mutation testing (optional, later) | PIT (`org.pitest:pitest-maven`) mutation score ≥ 70% for `service.ejb` |

### 6.4 Review checklist for new tests

- [ ] File is in the same package as the class under test, named `<Class>Test`
- [ ] Method names follow `method_scenario_expected`
- [ ] Happy path, each error return code, and boundary values covered
- [ ] Mocks verified for both expected and forbidden interactions
- [ ] No reliance on Liberty, DB2, CICS, network or real clock edge cases
- [ ] `mvn clean test jacoco:report` passes and coverage does not regress
