# Database Schema & Data Dictionary - DLIS

## Schema: `DLIS`
Target Database: **IBM DB2 for z/OS v12+** / **IBM DB2 LUW 11.5 (zLinux)**

---

## 1. Table Definitions

### 1.1 `DLIS.CANDIDATE`
Stores applicant demographic and identity records.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `CANDIDATE_ID` | `DECIMAL(10,0)` | No | `GENERATED ALWAYS AS IDENTITY` | Primary Key |
| `FIRST_NAME` | `VARCHAR(30)` | No | - | Applicant given name |
| `LAST_NAME` | `VARCHAR(30)` | No | - | Applicant family name |
| `DATE_OF_BIRTH` | `DATE` | No | - | Check: `DATE_OF_BIRTH < CURRENT DATE` |
| `ID_NUMBER` | `VARCHAR(20)` | No | - | National / Passport identification number |
| `ADDRESS_LINE_1`| `VARCHAR(50)` | No | - | Primary address line |
| `ADDRESS_LINE_2`| `VARCHAR(50)` | Yes | - | Secondary address line |
| `CITY` | `VARCHAR(30)` | No | - | City name |
| `STATE_PROVINCE`| `VARCHAR(30)`| No | - | State or province |
| `POSTAL_CODE` | `VARCHAR(10)` | No | - | Postal / ZIP code |
| `COUNTRY` | `VARCHAR(30)` | No | - | Country name |
| `PHONE_NUMBER` | `VARCHAR(15)` | Yes | - | Contact phone number |
| `EMAIL_ADDRESS` | `VARCHAR(60)` | Yes | - | Contact email |
| `CREATED_DATE` | `DATE` | No | - | Record registration date |
| `RECORD_STATUS` | `CHAR(1)` | No | `'A'` | Check: `'A'` (Active), `'I'` (Inactive) |

---

### 1.2 `DLIS.LICENSE_APPLICATION`
Tracks license applications through their multi-stage approval lifecycle.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `APPLICATION_ID` | `DECIMAL(10,0)` | No | `GENERATED ALWAYS (START 1000)` | Primary Key |
| `CANDIDATE_ID` | `DECIMAL(10,0)` | No | - | FK to `DLIS.CANDIDATE(CANDIDATE_ID)` |
| `LICENSE_TYPE` | `CHAR(1)` | No | - | Check: `'L'` (Learner), `'P'` (Probation), `'O'` (Open) |
| `APPLICATION_DATE` | `DATE` | No | - | Date application was logged |
| `APPLICATION_STATUS`| `CHAR(2)` | No | `'PE'` | Check: `'PE','EC','HC','PP','PA','A1','A2','AP','RE','IS'` |
| `ELIGIBILITY_CHK_STATUS`| `CHAR(1)` | Yes | `'U'` | Check: `'P'` (Pass), `'F'` (Fail), `'U'` (Unchecked) |
| `ELIGIBILITY_CHK_DATE` | `DATE` | Yes | - | Timestamp of eligibility check |
| `ELIGIBILITY_CHK_NOTES` | `VARCHAR(200)`| Yes | - | Eligibility check notes / failure reasons |
| `HISTORY_CHK_STATUS` | `CHAR(1)` | Yes | `'U'` | Check: `'P'` (Pass), `'F'` (Fail), `'U'` (Unchecked) |
| `HISTORY_CHK_DATE` | `DATE` | Yes | - | Timestamp of history check |
| `HISTORY_CHK_NOTES` | `VARCHAR(200)`| Yes | - | Background check violation notes |
| `PAYMENT_STATUS` | `CHAR(1)` | Yes | `'U'` | Check: `'P'` (Paid), `'U'` (Unpaid), `'W'` (Waived) |
| `PAYMENT_REFERENCE` | `VARCHAR(20)` | Yes | - | Transaction ID from payment processor |
| `APPROVAL_1_STATUS` | `CHAR(1)` | Yes | `'U'` | Check: `'A'` (Approved), `'R'` (Rejected), `'U'` (Pending) |
| `APPROVAL_1_AUTHORITY`| `VARCHAR(50)`| Yes | - | Name of Level 1 signing authority |
| `APPROVAL_1_DATE` | `DATE` | Yes | - | Date of first approval |
| `APPROVAL_1_NOTES` | `VARCHAR(200)`| Yes | - | First approval justification |
| `APPROVAL_2_STATUS` | `CHAR(1)` | Yes | `'U'` | Check: `'A'` (Approved), `'R'` (Rejected), `'U'` (Pending) |
| `APPROVAL_2_AUTHORITY`| `VARCHAR(50)`| Yes | - | Name of Level 2 signing supervisor |
| `APPROVAL_2_DATE` | `DATE` | Yes | - | Date of second approval |
| `APPROVAL_2_NOTES` | `VARCHAR(200)`| Yes | - | Second approval justification |
| `REJECTION_REASON` | `VARCHAR(300)`| Yes | - | Final rejection explanation |
| `CREATED_DATE` | `DATE` | No | - | Record created date |
| `LAST_UPDATED_DATE`| `DATE` | No | - | Record last modified date |
| `CREATED_BY` | `VARCHAR(20)` | No | - | Creator username / transaction |
| `LAST_UPDATED_BY` | `VARCHAR(20)` | No | - | Last modified username |

---

### 1.3 `DLIS.DRIVING_HISTORY`
Maintains traffic citations, offenses, suspensions, and demerit records.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `HISTORY_ID` | `DECIMAL(10,0)` | No | `GENERATED ALWAYS` | Primary Key |
| `CANDIDATE_ID` | `DECIMAL(10,0)` | No | - | FK to `DLIS.CANDIDATE` |
| `INCIDENT_DATE` | `DATE` | No | - | Date offense occurred |
| `INCIDENT_TYPE` | `CHAR(2)` | No | - | Check: `'OF'` (Offense), `'AC'` (Accident), `'SU'` (Suspension), `'DQ'` (Disqualification) |
| `INCIDENT_DESCRIPTION`| `VARCHAR(300)`| Yes | - | Incident details |
| `DEMERIT_POINTS` | `DECIMAL(3,0)` | Yes | `0` | Demerit points assessed ($\ge 0$) |
| `FINE_AMOUNT` | `DECIMAL(10,2)`| Yes | `0` | Monetary fine assessed ($\ge 0$) |
| `FINE_PAID_STATUS` | `CHAR(1)` | Yes | - | Check: `'Y'` (Yes), `'N'` (No) |
| `SUSPENSION_START_DATE`| `DATE` | Yes | - | Suspension start date |
| `SUSPENSION_END_DATE` | `DATE` | Yes | - | Suspension end date |
| `COURT_CASE_NUMBER` | `VARCHAR(20)` | Yes | - | Associated court case number |
| `RECORDED_BY_AUTHORITY`| `VARCHAR(50)`| Yes | - | Law enforcement agency |
| `RECORD_STATUS` | `CHAR(1)` | No | `'A'` | Check: `'A'` (Active), `'I'` (Inactive) |

---

### 1.4 `DLIS.ISSUED_LICENSE`
Represents official authorized driver licenses.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `LICENSE_ID` | `DECIMAL(10,0)` | No | `GENERATED ALWAYS` | Primary Key |
| `APPLICATION_ID` | `DECIMAL(10,0)` | No | - | FK to `DLIS.LICENSE_APPLICATION` |
| `CANDIDATE_ID` | `DECIMAL(10,0)` | No | - | FK to `DLIS.CANDIDATE` |
| `LICENSE_NUMBER` | `VARCHAR(20)` | No | - | Unique license identifier (`UQ_LIC_NUMBER`) |
| `LICENSE_TYPE` | `CHAR(1)` | No | - | Check: `'L'`, `'P'`, `'O'` |
| `ISSUE_DATE` | `DATE` | No | - | Date of issuance |
| `EXPIRY_DATE` | `DATE` | No | - | Check: `EXPIRY_DATE > ISSUE_DATE` |
| `LICENSE_STATUS` | `CHAR(1)` | No | `'A'` | Check: `'A'` (Active), `'S'` (Suspended), `'E'` (Expired), `'C'` (Cancelled), `'R'` (Renewed) |
| `VEHICLE_CLASS` | `CHAR(2)` | No | - | Check: `'A'`, `'B'`, `'C'`, `'D'` |
| `RESTRICTIONS` | `VARCHAR(200)`| Yes | - | Driving condition codes |
| `DEMERIT_BALANCE` | `DECIMAL(3,0)` | Yes | `12` | Available demerit point balance ($\ge 0$) |
| `ISSUED_BY_AUTHORITY`| `VARCHAR(50)`| No | - | Issuing authority branch |
| `ISSUED_BY_OFFICER` | `VARCHAR(50)` | No | - | Issuing licensing officer |
| `RENEWAL_COUNT` | `DECIMAL(3,0)` | Yes | `0` | Number of renewals performed |
| `PREVIOUS_LICENSE_ID`| `DECIMAL(10,0)`| Yes | - | Preceding license ID in upgrade chain |
| `NOTES` | `VARCHAR(300)`| Yes | - | Additional administrative notes |

---

### 1.5 `DLIS.PAYMENT`
Records all financial transactions related to license issuance and renewals.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `PAYMENT_ID` | `DECIMAL(10,0)` | No | `GENERATED ALWAYS` | Primary Key |
| `APPLICATION_ID` | `DECIMAL(10,0)` | No | - | FK to `DLIS.LICENSE_APPLICATION` |
| `CANDIDATE_ID` | `DECIMAL(10,0)` | No | - | FK to `DLIS.CANDIDATE` |
| `PAYMENT_DATE` | `DATE` | No | - | Date payment processed |
| `PAYMENT_AMOUNT` | `DECIMAL(10,2)`| No | - | Check: `PAYMENT_AMOUNT > 0` |
| `PAYMENT_METHOD` | `CHAR(2)` | No | - | Check: `'CC'` (Credit Card), `'DC'` (Debit Card), `'EF'` (EFT), `'CS'` (Cash), `'CH'` (Cheque) |
| `PAYMENT_REFERENCE`| `VARCHAR(30)` | No | - | Gateway / Bank transaction reference |
| `PAYMENT_STATUS` | `CHAR(1)` | No | `'P'` | Check: `'S'` (Success), `'F'` (Failed), `'R'` (Refunded), `'P'` (Pending) |
| `LICENSE_TYPE` | `CHAR(1)` | No | - | Check: `'L'`, `'P'`, `'O'` |
| `FEE_TYPE` | `CHAR(2)` | No | - | Check: `'IF'` (Issue Fee), `'RF'` (Renewal Fee), `'LF'` (Late Fee), `'PF'` (Processing Fee) |
| `RECEIPT_NUMBER` | `VARCHAR(20)` | Yes | - | Unique system-generated receipt string |
| `PROCESSED_BY` | `VARCHAR(20)` | Yes | - | Operator / Terminal processing payment |

---

### 1.6 `DLIS.LICENSE_FEE_SCHEDULE`
Statutory rate card defining license fee tariffs.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `FEE_SCHEDULE_ID`| `DECIMAL(10,0)` | No | `GENERATED ALWAYS` | Primary Key |
| `LICENSE_TYPE` | `CHAR(1)` | No | - | Check: `'L'`, `'P'`, `'O'` |
| `FEE_TYPE` | `CHAR(2)` | No | - | Check: `'IF'`, `'RF'`, `'LF'`, `'PF'` |
| `FEE_AMOUNT` | `DECIMAL(10,2)`| No | - | Check: `FEE_AMOUNT > 0` |
| `EFFECTIVE_DATE` | `DATE` | No | - | Tariff effective date |
| `EXPIRY_DATE` | `DATE` | Yes | - | Tariff sunset date |
| `CURRENCY_CODE` | `CHAR(3)` | No | `'USD'` | Standard ISO currency code |
| `ACTIVE_STATUS` | `CHAR(1)` | No | `'A'` | Check: `'A'` (Active), `'I'` (Inactive) |

---

### 1.7 `DLIS.AUTHORITY_USER`
Authorization directory for officers and supervisors performing workflow approvals.

| Column Name | Data Type | Nullable | Default | Constraints / Descriptions |
|---|---|---|---|---|
| `AUTHORITY_USER_ID`| `DECIMAL(10,0)`| No | `GENERATED ALWAYS` | Primary Key |
| `USER_CODE` | `VARCHAR(20)` | No | - | Unique officer identifier (`UQ_AUTHUSER_CODE`) |
| `USER_NAME` | `VARCHAR(60)` | No | - | Officer display name |
| `AUTHORITY_NAME` | `VARCHAR(50)` | No | - | Department / Jurisdiction branch |
| `AUTHORITY_LEVEL`| `CHAR(1)` | No | - | Check: `'1'` (First Approver), `'2'` (Final Approver) |
| `LICENSE_TYPES_AUTHORISED`| `VARCHAR(3)`| Yes | - | Substring pattern containing authorized types (e.g. `'LPO'`) |
| `ACTIVE_STATUS` | `CHAR(1)` | No | `'A'` | Check: `'A'` (Active), `'I'` (Inactive) |
