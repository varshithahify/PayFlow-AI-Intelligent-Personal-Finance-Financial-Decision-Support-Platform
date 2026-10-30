CREATE TABLE ai_investigation_findings (
    id BIGSERIAL PRIMARY KEY,

    org_id BIGINT NOT NULL,
    investigation_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,

    likely_cause VARCHAR(2000) NOT NULL,
    evidence TEXT NOT NULL,
    recommended_actions TEXT NOT NULL,
    limitations TEXT NOT NULL,

    requires_human_review BOOLEAN NOT NULL DEFAULT TRUE,
    model_name VARCHAR(100) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_findings_investigation
        FOREIGN KEY (investigation_id)
        REFERENCES investigations(id),

    CONSTRAINT fk_ai_findings_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id)
);

CREATE INDEX idx_ai_findings_org_investigation
    ON ai_investigation_findings (org_id, investigation_id);

CREATE INDEX idx_ai_findings_org_transaction
    ON ai_investigation_findings (org_id, transaction_id);

