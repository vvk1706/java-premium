# CICS Bridge & COMMAREA Interface Specification

## 1. Overview

The `dlis-cics-bridge` module enables legacy 3270 transactions, CICS DPL (Distributed Program Link) calls, and COBOL/PL/I batch programs to interact seamlessly with DLIS Java Enterprise Services. It implements CICS Java `Executable` programs that deserialize byte buffers (COMMAREA), delegate operations to stateless EJBs, and serialize response structures back into fixed-width EBCDIC/ASCII memory buffers.

---

## 2. CICS Transaction & Program Mapping

| Transaction Code | Program Name | Java Implementation Class | Functional Domain |
|---|---|---|---|
| **`CAND`** | `DLISCAND` | [`DlisCandProgram`](dlis-cics-bridge/src/main/java/com/dlis/cics/program/DlisCandProgram.java:1) | Candidate Onboarding & Inquiries |
| **`APPL`** | `DLISAPPL` | [`DlisApplProgram`](dlis-cics-bridge/src/main/java/com/dlis/cics/program/DlisApplProgram.java:1) | Application Lifecycle & Eligibility / History Checks |
| **`PAAP`** | `DLISPAAP` | [`DlisPayApprProgram`](dlis-cics-bridge/src/main/java/com/dlis/cics/program/DlisPayApprProgram.java:1) | Payment Processing, Dual Approvals & Issuance |

---

## 3. Standard COMMAREA Header (Base Header)

All DLIS COMMAREA structures share a common 108-byte header:

| Field Name | Byte Offset | Length | Data Type | Description |
|---|---|---|---|---|
| `TRAN-CODE` | 0 | 4 | `CHAR(4)` | Sub-function / Transaction identifier |
| `RETURN-CODE` | 4 | 4 | `PIC 9(4)` | Execution return code (`0000` = Success) |
| `RETURN-MESSAGE`| 8 | 100 | `CHAR(100)` | Textual status or error diagnostic message |
| **Total Header Length** | - | **108** | - | - |

---

## 4. COMMAREA Buffer Layouts

### 4.1 Application Commarea (`DLISAPPL`) - Buffer Size: 160 Bytes
- **Sub-functions (`TRAN-CODE`):**
  - `APLN`: Create Application
  - `APEC`: Run Eligibility Check
  - `APHC`: Run History Check
  - `APIS`: Inquire Status

| Field Name | Offset | Length | Format | Notes |
|---|---|---|---|---|
| `BASE-HEADER` | 0 | 108 | Composite | Standard return code and diagnostic message |
| `USER-ID` | 108 | 20 | `CHAR(20)` | User or operator executing the call |
| `APPLICATION-ID` | 128 | 10 | `PIC 9(10)` | Generated or requested Application ID |
| `CANDIDATE-ID` | 138 | 10 | `PIC 9(10)` | Associated Candidate ID |
| `LICENSE-TYPE` | 148 | 1 | `CHAR(1)` | License Category: `L`, `P`, or `O` |
| `APPLICATION-STATUS` | 149 | 2 | `CHAR(2)` | Application Status Code (`PE`..`IS`) |
| `ELIGIBILITY-STATUS` | 151 | 1 | `CHAR(1)` | Eligibility Check Flag (`P`, `F`, `U`) |
| `HISTORY-STATUS` | 152 | 1 | `CHAR(1)` | History Check Flag (`P`, `F`, `U`) |
| `PAYMENT-STATUS` | 153 | 1 | `CHAR(1)` | Payment Status Flag (`P`, `U`, `W`) |
| `APPROVAL-1-STATUS`| 154 | 1 | `CHAR(1)` | First Approval Decision (`A`, `R`, `U`) |
| `APPROVAL-2-STATUS`| 155 | 1 | `CHAR(1)` | Second Approval Decision (`A`, `R`, `U`) |
| `PAYMENT-REFERENCE`| 156 | 20 | `CHAR(20)`| Bank / Payment Gateway Reference |

### 4.2 Payment & Approval Commarea (`DLISPAAP`) - Buffer Size: 544 Bytes
- **Sub-functions (`TRAN-CODE`):**
  - `PAYM`: Process Payment
  - `APR1`: Record Level 1 Approval
  - `APR2`: Record Level 2 Approval
  - `ISSU`: Issue License

| Field Name | Offset | Length | Format | Notes |
|---|---|---|---|---|
| `BASE-HEADER` | 0 | 108 | Composite | Return Code & Status Message |
| `USER-ID` | 108 | 20 | `CHAR(20)` | Operator ID / Authority User Code |
| `APPLICATION-ID` | 128 | 10 | `PIC 9(10)` | Application Identifier |
| `CANDIDATE-ID` | 138 | 10 | `PIC 9(10)` | Candidate Identifier |
| `PAYMENT-METHOD` | 148 | 2 | `CHAR(2)` | Payment Method Code (`CC`, `DC`, etc.) |
| `PAYMENT-REFERENCE` | 150 | 30 | `CHAR(30)` | External Payment Reference |
| `RECEIPT-NUMBER` | 180 | 20 | `CHAR(20)` | Generated Receipt Identifier |
| `FEE-AMOUNT` | 200 | 10 | `PIC 9(8)V99`| Fee charged (implied 2 decimal places) |
| `DECISION` | 210 | 1 | `CHAR(1)` | Decision flag (`A` = Approve, `R` = Reject) |
| `DECISION-NOTES` | 211 | 200 | `CHAR(200)` | Officer decision justification |
| `VEHICLE-CLASS` | 411 | 2 | `CHAR(2)` | Target Vehicle Class (`A`, `B`, `C`, `D`) |
| `LICENSE-NUMBER` | 413 | 20 | `CHAR(20)` | Issued License Number (Output) |
| `LICENSE-ID` | 433 | 10 | `PIC 9(10)` | Generated License Primary Key |
| `ISSUED-BY-AUTHORITY`| 443 | 50 | `CHAR(50)` | Issuing Department Name |
| `ISSUED-BY-OFFICER` | 493 | 50 | `CHAR(50)` | Issuing Officer Full Name |
