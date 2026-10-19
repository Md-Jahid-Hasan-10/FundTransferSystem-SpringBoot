-- Readable ID counters (CUST1001, ACC1001, REQ1001, TXN1001)
CREATE SEQUENCE customer_code_seq START WITH 1001;
CREATE SEQUENCE account_number_seq START WITH 1001;
CREATE SEQUENCE request_code_seq START WITH 1001;
CREATE SEQUENCE transaction_code_seq START WITH 1001;

CREATE TABLE customers (
                           id            BIGSERIAL PRIMARY KEY,
                           customer_code VARCHAR(20)  NOT NULL UNIQUE,
                           name          VARCHAR(100) NOT NULL,
                           email         VARCHAR(150) NOT NULL UNIQUE,
                           phone         VARCHAR(30)  NOT NULL UNIQUE,
                           pin_hash      VARCHAR(100) NOT NULL
);

CREATE TABLE accounts (
                          id              BIGSERIAL PRIMARY KEY,
                          account_number  VARCHAR(20)   NOT NULL UNIQUE,
                          owner_id        BIGINT REFERENCES customers (id),
                          role            VARCHAR(30)   NOT NULL,
                          status          VARCHAR(20)   NOT NULL,
                          balance         NUMERIC(19,2) NOT NULL DEFAULT 0,
                          minimum_balance NUMERIC(19,2) NOT NULL DEFAULT 0,
                          version         BIGINT        NOT NULL DEFAULT 0,
                          CONSTRAINT chk_balance_not_negative CHECK (balance >= 0),
                          CONSTRAINT chk_owner_matches_role CHECK (
                              (role = 'CUSTOMER' AND owner_id IS NOT NULL) OR
                              (role = 'BANK_FEE_COLLECTION' AND owner_id IS NULL))
);

CREATE TABLE transaction_requests (
                                      id                     BIGSERIAL PRIMARY KEY,
                                      request_code           VARCHAR(20)   NOT NULL UNIQUE,
                                      customer_id            BIGINT        NOT NULL REFERENCES customers (id),
                                      type                   VARCHAR(20)   NOT NULL,
                                      source_account_id      BIGINT        NOT NULL REFERENCES accounts (id),
                                      destination_account_id BIGINT REFERENCES accounts (id),
                                      amount                 NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                                      fee                    NUMERIC(19,2) NOT NULL CHECK (fee >= 0),
                                      status                 VARCHAR(30)   NOT NULL,
                                      created_at             TIMESTAMPTZ   NOT NULL,
                                      expires_at             TIMESTAMPTZ   NOT NULL
);

CREATE TABLE transactions (
                              id                     BIGSERIAL PRIMARY KEY,
                              transaction_code       VARCHAR(20)   NOT NULL UNIQUE,
                              type                   VARCHAR(20)   NOT NULL,
                              source_account_id      BIGINT REFERENCES accounts (id),
                              destination_account_id BIGINT REFERENCES accounts (id),
                              amount                 NUMERIC(19,2) NOT NULL CHECK (amount > 0),
                              fee                    NUMERIC(19,2) NOT NULL CHECK (fee >= 0),
                              status                 VARCHAR(20)   NOT NULL,
                              created_at             TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_transactions_source      ON transactions (source_account_id);
CREATE INDEX idx_transactions_destination ON transactions (destination_account_id);
CREATE INDEX idx_transactions_created_at  ON transactions (created_at);

CREATE TABLE otp_records (
                             id         BIGSERIAL PRIMARY KEY,
                             request_id BIGINT       NOT NULL UNIQUE REFERENCES transaction_requests (id),
                             otp_hash   VARCHAR(100) NOT NULL,
                             expires_at TIMESTAMPTZ  NOT NULL,
                             attempts   INT          NOT NULL DEFAULT 0,
                             used       BOOLEAN      NOT NULL DEFAULT FALSE
);