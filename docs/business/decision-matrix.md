# Decision Logic and Validation Matrix - DLIS

## 1. Statutory Validation Matrix

This matrix details all business assertions, input criteria, and validation outcome codes.

| Code | Functional Area | Input Parameter | Validation Assertion | Success Path | Failure Code / Action |
|---|---|---|---|---|---|
| **VAL-01** | Candidate Creation | `idNumber` | Non-null, non-blank, unique in database | Create candidate record | `RC_DUPLICATE` (3): "CANDIDATE WITH THIS ID NUMBER ALREADY EXISTS" |
| **VAL-02** | Candidate Creation | `dateOfBirth` | $\text{DOB} < \text{CURRENT\_DATE}$ | Calculate Age | `RC_INVALID_AGE` (4): "DATE OF BIRTH MUST BE IN THE PAST" |
| **VAL-03** | Application Creation | `candidateId` | Candidate exists and `RECORD_STATUS = 'A'` | Proceed | `RC_NOT_FOUND` (1): "CANDIDATE NOT FOUND OR INACTIVE" |
| **VAL-04** | Application Creation | `licenseType` | Must match regex `[LPO]` | Proceed | `RC_INVALID_TYPE` (4): "INVALID LICENSE TYPE. MUST BE L, P OR O" |
| **VAL-05** | Application Creation | `(candidateId, licenseType)` | No existing active application (`PE..AP`) | Insert Application | `RC_ALREADY_EXISTS` (3): "AN ACTIVE APPLICATION ALREADY EXISTS FOR THIS LICENSE TYPE" |
| **VAL-06** | Eligibility Check | `licenseType == 'L'` | $\text{Age} \ge 16$ | Status $\rightarrow$ `EC` | Set Status $\rightarrow$ `RE`, Reason: "CANDIDATE MUST BE AT LEAST 16 YEARS OLD FOR LEARNER LICENSE" |
| **VAL-07** | Eligibility Check | `licenseType == 'P'` | $\text{Age} \ge 17$ and holds active `L` license | Status $\rightarrow$ `EC` | Set Status $\rightarrow$ `RE`, Reason: Age deficit or "CANDIDATE MUST HOLD AN ACTIVE LEARNER LICENSE TO APPLY FOR PROBATION" |
| **VAL-08** | Eligibility Check | `licenseType == 'O'` | $\text{Age} \ge 18$ and holds active `P` license | Status $\rightarrow$ `EC` | Set Status $\rightarrow$ `RE`, Reason: Age deficit or "CANDIDATE MUST HOLD AN ACTIVE PROBATION LICENSE TO APPLY FOR OPEN" |
| **VAL-09** | History Check | `applicationId` | `ELIGIBILITY_CHK_STATUS == 'P'` | Proceed check | RC 2: "ELIGIBILITY CHECK MUST BE PASSED BEFORE HISTORY CHECK" |
| **VAL-10** | History Check | Incident records | No active suspension (`SU`) / disqualification (`DQ`) | Continue checks | Set Status $\rightarrow$ `RE`, Reason: "CANDIDATE IS UNDER AN ACTIVE SUSPENSION OR DISQUALIFICATION" |
| **VAL-11** | History Check | Incident records | No unpaid fines (`FINE_PAID_STATUS == 'N'` and `FINE_AMOUNT > 0`) | Continue checks | Set Status $\rightarrow$ `RE`, Reason: "CANDIDATE HAS OUTSTANDING UNPAID FINES" |
| **VAL-12** | History Check | Demerit balance | Total accumulated demerit points $\le 12$ | Status $\rightarrow$ `HC` | Set Status $\rightarrow$ `RE`, Reason: "TOTAL DEMERIT POINTS EXCEED ALLOWABLE THRESHOLD OF 12" |
| **VAL-13** | Payment Processing | `applicationId` | `HISTORY_CHK_STATUS == 'P'` and `PAYMENT_STATUS != 'P'` | Calculate fee & insert payment | Error RC 2 or `RC_ALREADY_PAID` (3) |
| **VAL-14** | Payment Processing | `paymentMethod` | Must be `CC`, `DC`, `EF`, `CS`, `CH` | Generate Receipt | SQL Check constraint error if invalid |
| **VAL-15** | Approval 1 | `authorityUserCode` | User active, Level `'1'`, authorized for license type | Status $\rightarrow$ `A2` or `RE` | `RC_NOT_FOUND` (1) or `RC_NOT_AUTHORISED` (3) |
| **VAL-16** | Approval 2 | `authorityUserCode` | User active, Level `'2'`, authorized for license type | Status $\rightarrow$ `AP` or `RE` | `RC_NOT_FOUND` (1) or `RC_NOT_AUTHORISED` (3) |
| **VAL-17** | License Issuance | Pre-conditions | Status `AP`, Elig `P`, Hist `P`, Pay `P`, VehicleClass `[ABCD]` | Insert `ISSUED_LICENSE`, Status $\rightarrow$ `IS` | Error RC 2 / 3 |

---

## 2. Decision Logic Tables

### Decision Table 1: Driver License Eligibility Evaluation

| Condition / Action | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 | Rule 6 | Rule 7 |
|---|---|---|---|---|---|---|---|
| **License Type Requested** | `L` | `L` | `P` | `P` | `P` | `O` | `O` |
| **Candidate Age** | $\ge 16$ | $< 16$ | $\ge 17$ | $\ge 17$ | $< 17$ | $\ge 18$ | $< 18$ |
| **Active Prior License Held** | N/A | N/A | `L` | None / Expired | Any | `P` | Any |
| **Eligibility Outcome (`ELIGIBILITY_CHK_STATUS`)** | **`P` (Pass)** | **`F` (Fail)** | **`P` (Pass)** | **`F` (Fail)** | **`F` (Fail)** | **`P` (Pass)** | **`F` (Fail)** |
| **Application Status (`APPLICATION_STATUS`)** | **`EC`** | **`RE`** | **`EC`** | **`RE`** | **`RE`** | **`EC`** | **`RE`** |

### Decision Table 2: Driving History Background Clearance

| Condition / Action | Rule 1 | Rule 2 | Rule 3 | Rule 4 |
|---|---|---|---|---|
| **Active Suspension / Disqualification (`SU`/`DQ`)** | No | Yes | No | No |
| **Outstanding Unpaid Fines (`FINE_PAID_STATUS = 'N'`)** | No | Irrelevant | Yes | No |
| **Total Cumulative Demerit Points** | $\le 12$ | Irrelevant | Irrelevant | $> 12$ |
| **History Outcome (`HISTORY_CHK_STATUS`)** | **`P` (Pass)** | **`F` (Fail)** | **`F` (Fail)** | **`F` (Fail)** |
| **Application Status (`APPLICATION_STATUS`)** | **`HC`** | **`RE`** | **`RE`** | **`RE`** |

### Decision Table 3: License Expiry & Prefix Resolution

| License Type | Prefix | Vehicle Class | Validity Period | Initial Demerit Balance | Status on Issue |
|---|---|---|---|---|---|
| **Learner (`L`)** | `LRN` | A, B, C, D | 1 Year ($\text{Issue Date} + 1\text{ yr}$) | 12 Points | `A` (Active) |
| **Probation (`P`)** | `PRB` | A, B, C, D | 2 Years ($\text{Issue Date} + 2\text{ yrs}$) | 12 Points | `A` (Active) |
| **Open (`O`)** | `OPN` | A, B, C, D | 5 Years ($\text{Issue Date} + 5\text{ yrs}$) | 12 Points | `A` (Active) |
