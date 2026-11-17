-- ============================================================
-- V13 - Settlement Job Management
-- PayFlow AI
-- ============================================================

-- 1. SETTLEMENT JOBS
CREATE TABLE settlement_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL UNIQUE,
    org_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    total_transactions INT NOT NULL,
    processed_count INT NOT NULL DEFAULT 0,
    discrepancy_count INT NOT NULL DEFAULT 0,

    error_message VARCHAR(2000),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_settlement_jobs_organization
        FOREIGN KEY (org_id)
        REFERENCES organizations(id),

    CONSTRAINT chk_settlement_job_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_settlement_job_total
        CHECK (total_transactions > 0),

    CONSTRAINT chk_settlement_job_processed
        CHECK (processed_count >= 0),

    CONSTRAINT chk_settlement_job_discrepancies
        CHECK (discrepancy_count >= 0)
);

CREATE INDEX idx_settlement_jobs_org_created
    ON settlement_jobs(org_id, created_at DESC);

CREATE INDEX idx_settlement_jobs_org_status
    ON settlement_jobs(org_id, status);


-- 2. TRANSACTIONS ASSIGNED TO SETTLEMENT JOBS
CREATE TABLE settlement_job_transactions (
    id BIGSERIAL PRIMARY KEY,

    settlement_job_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_job_transactions_job
        FOREIGN KEY (settlement_job_id)
        REFERENCES settlement_jobs(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_job_transactions_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),

    CONSTRAINT uk_job_transaction
        UNIQUE (settlement_job_id, transaction_id)
);

CREATE INDEX idx_job_transactions_transaction
    ON settlement_job_transactions(transaction_id);


-- 3. SETTLEMENT JOB EXECUTION HISTORY
CREATE TABLE settlement_job_executions (
    id BIGSERIAL PRIMARY KEY,

    settlement_job_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',

    processed_count INT NOT NULL DEFAULT 0,
    discrepancy_count INT NOT NULL DEFAULT 0,

    error_message VARCHAR(2000),

    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_job_executions_job
        FOREIGN KEY (settlement_job_id)
        REFERENCES settlement_jobs(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_job_execution_attempt
        UNIQUE (settlement_job_id, attempt_number),

    CONSTRAINT chk_job_execution_attempt
        CHECK (attempt_number > 0),

    CONSTRAINT chk_job_execution_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk_job_execution_processed
        CHECK (processed_count >= 0),

    CONSTRAINT chk_job_execution_discrepancies
        CHECK (discrepancy_count >= 0)
);

CREATE INDEX idx_job_executions_job_started
    ON settlement_job_executions(settlement_job_id, started_at DESC);

-- ============================================================
-- END V13
-- ============================================================
