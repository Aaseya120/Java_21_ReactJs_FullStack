-- V5: Add mobile_number column to users table for User ID recovery
ALTER TABLE users ADD COLUMN IF NOT EXISTS mobile_number VARCHAR(20);
