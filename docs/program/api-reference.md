# REST API Specification - DLIS Web Services

Base URL: `http://<host>:<port>/dlis/api/v1`  
Content-Type: `application/json`

---

## 1. Candidate Endpoints

### 1.1 Create Candidate
- **Method:** `POST`
- **Path:** `/candidates`
- **Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "dateOfBirth": "1995-06-15",
  "idNumber": "DLIS-ID-99881",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 4B",
  "city": "Springfield",
  "stateProvince": "IL",
  "postalCode": "62701",
  "country": "USA",
  "phoneNumber": "555-0199",
  "emailAddress": "john.doe@example.com"
}
```
- **Responses:**
  - `201 Created`:
```json
{
  "id": 101,
  "message": "CANDIDATE CREATED SUCCESSFULLY"
}
```
  - `422 Unprocessable Entity`:
```json
{
  "returnCode": 3,
  "message": "CANDIDATE WITH THIS ID NUMBER ALREADY EXISTS"
}
```

### 1.2 Get Candidate by ID
- **Method:** `GET`
- **Path:** `/candidates/{id}`
- **Responses:**
  - `200 OK`: Candidate JSON object
  - `404 Not Found`: Candidate not found

### 1.3 Search Candidate by ID Number
- **Method:** `GET`
- **Path:** `/candidates/id-number/{idNumber}`
- **Responses:**
  - `200 OK`: Candidate JSON object
  - `404 Not Found`: Candidate not found

---

## 2. Application Endpoints

### 2.1 Create License Application
- **Method:** `POST`
- **Path:** `/applications`
- **Request Body:**
```json
{
  "candidateId": 101,
  "licenseType": "L",
  "createdBy": "OFFICER_A"
}
```
- **Responses:**
  - `201 Created`:
```json
{
  "id": 1001,
  "message": "APPLICATION CREATED SUCCESSFULLY"
}
```

### 2.2 Inquire Application Status
- **Method:** `GET`
- **Path:** `/applications/{id}`
- **Responses:**
  - `200 OK`:
```json
{
  "applicationId": 1001,
  "candidateId": 101,
  "licenseType": "L",
  "applicationDate": "2024-03-01",
  "applicationStatus": "PE",
  "eligibilityChkStatus": "U",
  "historyChkStatus": "U",
  "paymentStatus": "U",
  "approval1Status": "U",
  "approval2Status": "U"
}
```

### 2.3 Run Eligibility Check
- **Method:** `POST`
- **Path:** `/applications/{id}/eligibility-check`
- **Request Body:**
```json
{
  "checkedBy": "SYS_AUTO_EVAL"
}
```
- **Responses:**
  - `200 OK`:
```json
{
  "status": "P",
  "message": "ELIGIBILITY CHECK PASSED"
}
```

### 2.4 Run History Check
- **Method:** `POST`
- **Path:** `/applications/{id}/history-check`
- **Request Body:**
```json
{
  "checkedBy": "OFFICER_BG"
}
```
- **Responses:**
  - `200 OK`:
```json
{
  "status": "P",
  "message": "HISTORY CHECK PASSED"
}
```

---

## 3. Payment & Approval Endpoints

### 3.1 Record Payment
- **Method:** `POST`
- **Path:** `/applications/{id}/payment`
- **Request Body:**
```json
{
  "candidateId": 101,
  "paymentMethod": "CC",
  "paymentReference": "AUTH_99812481",
  "processedBy": "CASHIER_01"
}
```
- **Responses:**
  - `200 OK`:
```json
{
  "paymentId": 201,
  "receiptNumber": "RCP202403011001",
  "message": "PAYMENT RECORDED SUCCESSFULLY"
}
```

### 3.2 Record Level 1 Approval
- **Method:** `POST`
- **Path:** `/applications/{id}/approval1`
- **Request Body:**
```json
{
  "authorityUserCode": "AUTH1_USER",
  "decision": "A",
  "decisionNotes": "Applicant documentation fully verified"
}
```
- **Responses:**
  - `200 OK`:
```json
{
  "message": "FIRST APPROVAL RECORDED - APPLICATION FORWARDED FOR SECOND APPROVAL"
}
```

### 3.3 Record Level 2 Approval
- **Method:** `POST`
- **Path:** `/applications/{id}/approval2`
- **Request Body:**
```json
{
  "authorityUserCode": "SUPERVISOR_01",
  "decision": "A",
  "decisionNotes": "Supervisor signoff confirmed"
}
```
- **Responses:**
  - `200 OK`:
```json
{
  "message": "SECOND APPROVAL GRANTED - APPLICATION FULLY APPROVED AND READY FOR ISSUE"
}
```

### 3.4 Issue Driver License
- **Method:** `POST`
- **Path:** `/applications/{id}/issue`
- **Request Body:**
```json
{
  "vehicleClass": "B",
  "restrictions": "Corrective lenses required",
  "issuedByOfficer": "OFFICER_SMITH",
  "issuedByAuthority": "DMV_CENTRAL"
}
```
- **Responses:**
  - `201 Created`:
```json
{
  "licenseId": 5001,
  "applicationId": 1001,
  "candidateId": 101,
  "licenseNumber": "LRNB1011001",
  "licenseType": "L",
  "issueDate": "2024-03-01",
  "expiryDate": "2025-03-01",
  "licenseStatus": "A",
  "vehicleClass": "B",
  "restrictions": "Corrective lenses required",
  "demeritBalance": 12,
  "issuedByAuthority": "DMV_CENTRAL",
  "issuedByOfficer": "OFFICER_SMITH",
  "renewalCount": 0
}
```
