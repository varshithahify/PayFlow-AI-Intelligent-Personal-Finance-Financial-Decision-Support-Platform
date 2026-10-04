-- ============================================================
-- V8 - Multi-Tenant Database Schema
-- PayFlow AI
-- ============================================================

-- ============================================================
-- 1. ORGANIZATIONS
-- Multi-tenant root table.
-- Existing project uses BIGINT IDs, so we preserve that
-- strategy instead of converting existing data to UUIDs.
-- ============================================================

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(100),
    kyc_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    plan VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create one organization for the existing single-tenant data.
INSERT INTO organizations (
    id,
    name,
    business_type,
    kyc_status,
    plan,
    created_at
)
VALUES (
    1,
    'Legacy PayFlow Organization',
    'PAYMENT_MERCHANT',
    'PENDING',
    'STARTER',
    CURRENT_TIMESTAMP
);

-- Keep the sequence ahead of the manually inserted ID.
SELECT setval(
    pg_get_serial_sequence('organizations', 'id'),
    (SELECT MAX(id) FROM organizations)
);


-- ============================================================
-- 2. USERS
-- Add organization ownership to existing users.
-- ============================================================

ALTER TABLE users
ADD COLUMN org_id BIGINT;

UPDATE users
SET org_id = 1
WHERE org_id IS NULL;

ALTER TABLE users
ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE users
ADD CONSTRAINT fk_users_organization
FOREIGN KEY (org_id)
REFERENCES organizations(id);

CREATE INDEX idx_users_org_id
ON users(org_id);


-- ============================================================
-- 3. TRANSACTIONS
-- Add organization ownership to existing transactions.
-- ============================================================

ALTER TABLE transactions
ADD COLUMN org_id BIGINT;

UPDATE transactions
SET org_id = 1
WHERE org_id IS NULL;

ALTER TABLE transactions
ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE transactions
ADD CONSTRAINT fk_transactions_organization
FOREIGN KEY (org_id)
REFERENCES organizations(id);

CREATE INDEX idx_transactions_org_id
ON transactions(org_id);


-- ============================================================
-- 4. EXTERNAL PAYMENT RECORDS
-- Add organization ownership.
-- Existing records are assigned to the legacy organization.
-- ============================================================

ALTER TABLE external_payment_records
ADD COLUMN org_id BIGINT;

UPDATE external_payment_records
SET org_id = 1
WHERE org_id IS NULL;

ALTER TABLE external_payment_records
ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE external_payment_records
ADD CONSTRAINT fk_external_payment_records_organization
FOREIGN KEY (org_id)
REFERENCES organizations(id);

CREATE INDEX idx_external_payment_records_org_id
ON external_payment_records(org_id);


-- ============================================================
-- 5. RECONCILIATION RECORDS
-- Add organization ownership.
-- ============================================================

ALTER TABLE reconciliation_records
ADD COLUMN org_id BIGINT;

UPDATE reconciliation_records
SET org_id = 1
WHERE org_id IS NULL;

ALTER TABLE reconciliation_records
ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE reconciliation_records
ADD CONSTRAINT fk_reconciliation_records_organization
FOREIGN KEY (org_id)
REFERENCES organizations(id);

CREATE INDEX idx_reconciliation_records_org_id
ON reconciliation_records(org_id);


-- ============================================================
-- 6. INVESTIGATIONS
-- Add organization ownership.
-- ============================================================

ALTER TABLE investigations
ADD COLUMN org_id BIGINT;

UPDATE investigations
SET org_id = 1
WHERE org_id IS NULL;

ALTER TABLE investigations
ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE investigations
ADD CONSTRAINT fk_investigations_organization
FOREIGN KEY (org_id)
REFERENCES organizations(id);

CREATE INDEX idx_investigations_org_id
ON investigations(org_id);


-- ============================================================
-- 7. PROCESSED EVENTS
-- Scope processed events to the organization.
-- ============================================================

ALTER TABLE processed_events
ADD COLUMN org_id BIGINT;

UPDATE processed_events
SET org_id = 1
WHERE org_id IS NULL;

ALTER TABLE processed_events
ALTER COLUMN org_id SET NOT NULL;

ALTER TABLE processed_events
ADD CONSTRAINT fk_processed_events_organization
FOREIGN KEY (org_id)
REFERENCES organizations(id);

CREATE INDEX idx_processed_events_org_id
ON processed_events(org_id);


-- ============================================================
-- 8. GATEWAY CONFIGURATION
-- Merchant-specific gateway configuration.
-- ============================================================

CREATE TABLE gateway_configs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    gateway_name VARCHAR(50) NOT NULL,
    api_key_encrypted VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority_order INT NOT NULL DEFAULT 1,
    min_health_score DOUBLE PRECISION NOT NULL DEFAULT 0.7,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_gateway_configs_organization
        FOREIGN KEY (org_id)
        REFERENCES organizations(id)
);

CREATE INDEX idx_gateway_configs_org_id
ON gateway_configs(org_id);

CREATE INDEX idx_gateway_configs_org_active
ON gateway_configs(org_id, is_active);


-- ============================================================
-- 9. GATEWAY HEALTH
-- Gateway health metrics used for routing decisions.
-- ============================================================

CREATE TABLE gateway_health (
    id BIGSERIAL PRIMARY KEY,
    gateway_name VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success_rate DOUBLE PRECISION,
    avg_latency_ms INT,
    error_rate DOUBLE PRECISION,
    health_score DOUBLE PRECISION,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_gateway_health_gateway_recorded
ON gateway_health(gateway_name, recorded_at);


-- ============================================================
-- 10. FRAUD SCORES
-- Stores fraud scoring results associated with transactions.
-- Actual ML logic will be implemented in Phase 6.
-- ============================================================

CREATE TABLE fraud_scores (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    velocity_score DOUBLE PRECISION,
    geo_score DOUBLE PRECISION,
    device_score DOUBLE PRECISION,
    ml_score DOUBLE PRECISION,
    triggered_rules TEXT,
    reviewed_by BIGINT,
    review_decision VARCHAR(20),
    scored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_fraud_scores_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),

    CONSTRAINT fk_fraud_scores_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
);

CREATE INDEX idx_fraud_scores_transaction_id
ON fraud_scores(transaction_id);


-- ============================================================
-- 11. SETTLEMENT FILES
-- Stores gateway settlement files for reconciliation.
-- ============================================================

CREATE TABLE settlement_files (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    gateway_name VARCHAR(50),
    settlement_date DATE,
    file_url VARCHAR(500),
    total_amount NUMERIC(14, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_settlement_files_organization
        FOREIGN KEY (org_id)
        REFERENCES organizations(id)
);

CREATE INDEX idx_settlement_files_org_id
ON settlement_files(org_id);

CREATE INDEX idx_settlement_files_status
ON settlement_files(status);


-- ============================================================
-- 12. LINK EXISTING RECONCILIATION RECORDS TO SETTLEMENT FILES
-- Nullable because existing reconciliation records were created
-- before settlement files existed.
-- ============================================================

ALTER TABLE reconciliation_records
ADD COLUMN settlement_file_id BIGINT;

ALTER TABLE reconciliation_records
ADD CONSTRAINT fk_reconciliation_settlement_file
FOREIGN KEY (settlement_file_id)
REFERENCES settlement_files(id);

CREATE INDEX idx_reconciliation_settlement_file
ON reconciliation_records(settlement_file_id);


-- ============================================================
-- 13. WEBHOOK CONFIGURATION
-- Merchant webhook endpoints.
-- ============================================================

CREATE TABLE webhook_configs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT NOT NULL,
    endpoint_url VARCHAR(500) NOT NULL,
    secret_key VARCHAR(255),
    events TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_webhook_configs_organization
        FOREIGN KEY (org_id)
        REFERENCES organizations(id)
);

CREATE INDEX idx_webhook_configs_org_id
ON webhook_configs(org_id);

CREATE INDEX idx_webhook_configs_org_active
ON webhook_configs(org_id, is_active);


-- ============================================================
-- 14. AUDIT LOGS
-- Append-only audit trail for security and operational actions.
-- ============================================================

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    org_id BIGINT,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    details JSONB,
    ip_address INET,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_organization
        FOREIGN KEY (org_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_audit_logs_org_id
ON audit_logs(org_id);

CREATE INDEX idx_audit_logs_user_id
ON audit_logs(user_id);

CREATE INDEX idx_audit_logs_created_at
ON audit_logs(created_at);