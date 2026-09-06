# PayFlow AI
## Database Design

---

# 1. Database Overview

PayFlow AI uses PostgreSQL as the authoritative transactional
database.

PostgreSQL stores all critical financial, operational, security,
investigation, and audit information.

Redis is not used as the source of truth for financial records.

---

# 2. Database Design Principles

The database follows these principles:

1. PostgreSQL is the source of truth.
2. Financial records use transactional consistency.
3. Primary keys uniquely identify entities.
4. Foreign keys maintain referential integrity.
5. Unique constraints prevent duplicate business records.
6. Indexes support frequently executed queries.
7. Audit information is retained for important operations.
8. Historical records are not silently overwritten.
9. Critical state transitions are validated by the application and
   protected by database constraints where appropriate.
10. Database transactions are used for atomic business operations.

---

# 3. Main Entities

The initial database contains the following major entities:

```text
users
roles
user_roles

gateways

payments
payment_attempts
payment_events

settlements
reconciliation_results

investigation_cases
investigation_evidence
ai_analyses

resolution_actions
approvals

audit_logs
outbox_events