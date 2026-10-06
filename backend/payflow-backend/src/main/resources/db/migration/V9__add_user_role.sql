-- ============================================================
-- V9 - Add User Role
-- PayFlow AI
-- ============================================================

ALTER TABLE users
ADD COLUMN role VARCHAR(30);

UPDATE users
SET role = 'MERCHANT_ADMIN'
WHERE role IS NULL;

ALTER TABLE users
ALTER COLUMN role SET NOT NULL;