-- ============================================================
-- DLIS candidate service — DB2 DDL (tables owned by dlis-candidate-service)
-- Run with: db2 SET SCHEMA DLIS  (the schema is shared with the legacy monolith / CICS bridge)
-- No foreign keys cross a service boundary: candidate/application ids are validated
-- by REST calls, not by the database.
-- ============================================================

-- Table: CANDIDATE
CREATE TABLE CANDIDATE (
    CANDIDATE_ID        DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                        (START WITH 1, INCREMENT BY 1),
    FIRST_NAME          VARCHAR(30)     NOT NULL,
    LAST_NAME           VARCHAR(30)     NOT NULL,
    DATE_OF_BIRTH       DATE            NOT NULL,
    ID_NUMBER           VARCHAR(20)     NOT NULL,
    ADDRESS_LINE_1      VARCHAR(50)     NOT NULL,
    ADDRESS_LINE_2      VARCHAR(50),
    CITY                VARCHAR(30)     NOT NULL,
    STATE_PROVINCE      VARCHAR(30)     NOT NULL,
    POSTAL_CODE         VARCHAR(10)     NOT NULL,
    COUNTRY             VARCHAR(30)     NOT NULL,
    PHONE_NUMBER        VARCHAR(15),
    EMAIL_ADDRESS       VARCHAR(60),
    CREATED_DATE        DATE            NOT NULL,
    RECORD_STATUS       CHAR(1)         NOT NULL DEFAULT 'A',
    CONSTRAINT PK_CANDIDATE PRIMARY KEY (CANDIDATE_ID),
    CONSTRAINT CHK_CAND_STATUS  CHECK (RECORD_STATUS IN ('A','I')),
    CONSTRAINT CHK_CAND_DOB     CHECK (DATE_OF_BIRTH < CURRENT DATE)
);

-- Indexes
CREATE INDEX IDX_CAND_IDNUM   ON CANDIDATE     (ID_NUMBER);
CREATE INDEX IDX_CAND_NAME    ON CANDIDATE     (LAST_NAME, FIRST_NAME);
CREATE INDEX IDX_CAND_STATUS  ON CANDIDATE     (RECORD_STATUS);
