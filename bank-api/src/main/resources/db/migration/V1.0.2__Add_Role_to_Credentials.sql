-- Migration: Add 'role' column to credentials table
-- Required for RBAC (Role-Based Access Control).
-- Default 'CLIENT' — every existing credential becomes a regular client.
-- Promote specific users to 'ADMIN' manually via SQL when needed.

ALTER TABLE "credentials"
    ADD COLUMN "role" VARCHAR(20) NOT NULL DEFAULT 'CLIENT';