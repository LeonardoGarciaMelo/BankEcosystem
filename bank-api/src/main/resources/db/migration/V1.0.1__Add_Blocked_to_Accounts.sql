-- Migration: Add 'blocked' column to accounts table
-- Required for the Anti-Fraud system to lock suspicious accounts.
-- Default FALSE ensures all existing accounts remain active after migration.

ALTER TABLE "accounts"
    ADD COLUMN "blocked" BOOLEAN NOT NULL DEFAULT FALSE;