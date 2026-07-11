-- Migration: Add 'locked_until' column to credentials table
-- Supports account lockout after repeated failed login attempts (see AuthService).
-- NULL means the account is not currently locked.

ALTER TABLE "credentials"
    ADD COLUMN "locked_until" TIMESTAMP NULL;