CREATE TABLE failed_event_records (
    id BIGSERIAL PRIMARY KEY,

    event_id UUID NOT NULL UNIQUE,

    transaction_id BIGINT,

    org_id BIGINT,

    event_type VARCHAR(50) NOT NULL,

    failure_reason VARCHAR(1000),

    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_failed_event_records_organization
        FOREIGN KEY (org_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_failed_event_records_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id)
);

CREATE INDEX idx_failed_event_records_org_id
    ON failed_event_records(org_id);

CREATE INDEX idx_failed_event_records_transaction_id
    ON failed_event_records(transaction_id);