-- PayFlow AI: Database Performance Test Data
-- Generates 1,000 transactions for user 9, organization 1.

BEGIN;

INSERT INTO transactions (
    amount,
    currency,
    payment_method,
    status,
    user_id,
    org_id,
    idempotency_key,
    created_at,
    updated_at
)
SELECT
    (100 + (g % 9900))::NUMERIC(15, 2),
    'INR',

    CASE
        WHEN g % 2 = 0 THEN 'CARD'
        ELSE 'UPI'
    END,

    CASE
        WHEN g % 4 = 0 THEN 'CREATED'
        WHEN g % 4 = 1 THEN 'PROCESSING'
        WHEN g % 4 = 2 THEN 'SUCCESS'
        ELSE 'FAILED'
    END,

    9,
    1,
    'perf-seed-' || g,
    NOW() - (g * INTERVAL '1 minute'),
    NOW() - (g * INTERVAL '1 minute')

FROM generate_series(1, 1000) AS g;

-- Verify inserted test records
SELECT
    COUNT(*) AS seeded_transactions,
    COUNT(DISTINCT idempotency_key) AS unique_keys,
    MIN(created_at) AS oldest_transaction,
    MAX(created_at) AS newest_transaction
FROM transactions
WHERE user_id = 9
  AND org_id = 1
  AND idempotency_key LIKE 'perf-seed-%';

-- Review distribution
SELECT status, COUNT(*)
FROM transactions
WHERE idempotency_key LIKE 'perf-seed-%'
GROUP BY status
ORDER BY status;

-- Do not commit until results are verified.
COMMIT;