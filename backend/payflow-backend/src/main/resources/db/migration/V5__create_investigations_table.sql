CREATE TABLE investigations (
    id BIGSERIAL PRIMARY KEY,

    transaction_id BIGINT NOT NULL,

    reconciliation_record_id BIGINT,

    status VARCHAR(20) NOT NULL,

    priority VARCHAR(20) NOT NULL,

    issue_type VARCHAR(30) NOT NULL,

    summary VARCHAR(1000),

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    resolved_at TIMESTAMP,

    CONSTRAINT fk_investigation_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),

    CONSTRAINT fk_investigation_reconciliation
        FOREIGN KEY (reconciliation_record_id)
        REFERENCES reconciliation_records(id)
);

CREATE INDEX idx_investigations_transaction
    ON investigations(transaction_id);

CREATE INDEX idx_investigations_status
    ON investigations(status);

CREATE INDEX idx_investigations_priority
    ON investigations(priority);

CREATE INDEX idx_investigations_issue_type
    ON investigations(issue_type);