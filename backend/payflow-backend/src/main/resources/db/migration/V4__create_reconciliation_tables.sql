CREATE TABLE external_payment_records (
    id BIGSERIAL PRIMARY KEY,
    external_transaction_id VARCHAR(100) NOT NULL,
    internal_transaction_id BIGINT,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_external_transaction_id
        UNIQUE (external_transaction_id)
);

CREATE INDEX idx_external_internal_transaction
    ON external_payment_records(internal_transaction_id);


CREATE TABLE reconciliation_records (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT,
    external_record_id BIGINT,
    status VARCHAR(30) NOT NULL,
    internal_amount NUMERIC(15, 2),
    external_amount NUMERIC(15, 2),
    internal_status VARCHAR(20),
    external_status VARCHAR(20),
    mismatch_reason VARCHAR(500),
    reconciled_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_reconciliation_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),

    CONSTRAINT fk_reconciliation_external
        FOREIGN KEY (external_record_id)
        REFERENCES external_payment_records(id)
);

CREATE INDEX idx_reconciliation_transaction
    ON reconciliation_records(transaction_id);

CREATE INDEX idx_reconciliation_status
    ON reconciliation_records(status);