-- ============================================================
-- Fix: goals table has stale column 'appraisal_cycle' (NOT NULL, no default)
-- The entity uses @JoinColumn(name = "cycle_id") but the old column
-- was named 'appraisal_cycle' by Hibernate's default naming strategy.
-- This migration drops the stale column and ensures cycle_id is correct.
-- Run against: karmika_hrms database
-- ============================================================

USE karmika_hrms;

-- Step 1: Inspect current state (optional, for verification)
-- DESCRIBE goals;

-- Step 2: Drop the stale 'appraisal_cycle' column
--         (it has no default + NOT NULL, causing insert failures)
ALTER TABLE goals DROP COLUMN IF EXISTS appraisal_cycle;

-- Step 3: Ensure cycle_id column exists and is nullable (cycle is optional on a goal)
--         If it doesn't exist yet, Hibernate's update mode will create it on next boot.
--         If it already exists but is NOT NULL, make it nullable:
ALTER TABLE goals MODIFY COLUMN cycle_id BIGINT NULL;

-- Step 4: Verify final state
DESCRIBE goals;
