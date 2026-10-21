-- ============================================================
-- DLIS payment service — DB2 DDL (tables owned by dlis-payment-service)
-- Run with: db2 SET SCHEMA DLIS  (the schema is shared with the legacy monolith / CICS bridge)
-- No foreign keys cross a service boundary: candidate/application ids are validated
-- by REST calls, not by the database.
-- ============================================================

-- Table: PAYMENT
CREATE TABLE PAYMENT (
    PAYMENT_ID          DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                        (START WITH 1, INCREMENT BY 1),
    APPLICATION_ID      DECIMAL(10,0)   NOT NULL,
    CANDIDATE_ID        DECIMAL(10,0)   NOT NULL,
    PAYMENT_DATE        DATE            NOT NULL,
    PAYMENT_AMOUNT      DECIMAL(10,2)   NOT NULL,
    PAYMENT_METHOD      CHAR(2)         NOT NULL,
    PAYMENT_REFERENCE   VARCHAR(30)     NOT NULL,
    PAYMENT_STATUS      CHAR(1)         NOT NULL DEFAULT 'P',
    LICENSE_TYPE        CHAR(1)         NOT NULL,
    FEE_TYPE            CHAR(2)         NOT NULL,
    RECEIPT_NUMBER      VARCHAR(20),
    PROCESSED_BY        VARCHAR(20),
    NOTES               VARCHAR(200),
    CONSTRAINT PK_PAYMENT        PRIMARY KEY (PAYMENT_ID),
    CONSTRAINT CHK_PAY_METHOD    CHECK (PAYMENT_METHOD IN ('CC','DC','EF','CS','CH')),
    CONSTRAINT CHK_PAY_STATUS    CHECK (PAYMENT_STATUS IN ('S','F','R','P')),
    CONSTRAINT CHK_PAY_LICTYPE   CHECK (LICENSE_TYPE IN ('L','P','O')),
    CONSTRAINT CHK_PAY_FEETYPE   CHECK (FEE_TYPE IN ('IF','RF','LF','PF')),
    CONSTRAINT CHK_PAY_AMOUNT    CHECK (PAYMENT_AMOUNT > 0)
);

-- Table: LICENSE_FEE_SCHEDULE
CREATE TABLE LICENSE_FEE_SCHEDULE (
    FEE_SCHEDULE_ID     DECIMAL(10,0)   NOT NULL GENERATED ALWAYS AS IDENTITY
                                        (START WITH 1, INCREMENT BY 1),
    LICENSE_TYPE        CHAR(1)         NOT NULL,
    FEE_TYPE            CHAR(2)         NOT NULL,
    FEE_AMOUNT          DECIMAL(10,2)   NOT NULL,
    EFFECTIVE_DATE      DATE            NOT NULL,
    EXPIRY_DATE         DATE,
    CURRENCY_CODE       CHAR(3)         NOT NULL DEFAULT 'USD',
    DESCRIPTION         VARCHAR(200),
    ACTIVE_STATUS       CHAR(1)         NOT NULL DEFAULT 'A',
    CONSTRAINT PK_FEESCHED       PRIMARY KEY (FEE_SCHEDULE_ID),
    CONSTRAINT CHK_FEE_LICTYPE   CHECK (LICENSE_TYPE IN ('L','P','O')),
    CONSTRAINT CHK_FEE_FEETYPE   CHECK (FEE_TYPE IN ('IF','RF','LF','PF')),
    CONSTRAINT CHK_FEE_AMOUNT    CHECK (FEE_AMOUNT > 0),
    CONSTRAINT CHK_FEE_ACTIVE    CHECK (ACTIVE_STATUS IN ('A','I'))
);

-- Indexes
CREATE INDEX IDX_PAY_APP      ON PAYMENT          (APPLICATION_ID);
CREATE INDEX IDX_PAY_CAND     ON PAYMENT          (CANDIDATE_ID);
CREATE INDEX IDX_FEE_LOOKUP   ON LICENSE_FEE_SCHEDULE
    (LICENSE_TYPE, FEE_TYPE, ACTIVE_STATUS, EFFECTIVE_DATE, EXPIRY_DATE);

-- Reference data
INSERT INTO LICENSE_FEE_SCHEDULE
    (LICENSE_TYPE, FEE_TYPE, FEE_AMOUNT, EFFECTIVE_DATE, CURRENCY_CODE, DESCRIPTION, ACTIVE_STATUS)
VALUES
    ('L', 'IF', 45.00, '2024-01-01', 'USD', 'Learner License Issue Fee',     'A'),
    ('P', 'IF', 75.00, '2024-01-01', 'USD', 'Probation License Issue Fee',   'A'),
    ('O', 'IF', 95.00, '2024-01-01', 'USD', 'Open License Issue Fee',        'A'),
    ('L', 'RF', 40.00, '2024-01-01', 'USD', 'Learner License Renewal Fee',   'A'),
    ('P', 'RF', 65.00, '2024-01-01', 'USD', 'Probation License Renewal Fee', 'A'),
    ('O', 'RF', 85.00, '2024-01-01', 'USD', 'Open License Renewal Fee',      'A'),
    ('L', 'LF', 20.00, '2024-01-01', 'USD', 'Late Processing Fee - Learner', 'A'),
    ('P', 'LF', 25.00, '2024-01-01', 'USD', 'Late Processing Fee - Probation','A'),
    ('O', 'LF', 30.00, '2024-01-01', 'USD', 'Late Processing Fee - Open',    'A');

COMMIT;
