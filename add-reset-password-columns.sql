-- ============================================================
-- Migration: Add password reset columns to users table
-- Run against karmika_hrms database BEFORE restarting the backend
-- ============================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS reset_token VARCHAR(10) NULL,
    ADD COLUMN IF NOT EXISTS reset_token_expiry DATETIME NULL;

SELECT 'Password reset columns added successfully.' AS status;
