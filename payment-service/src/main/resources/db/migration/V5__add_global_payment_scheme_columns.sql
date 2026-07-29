-- V5: Add global payment scheme metadata columns for recurring mandates and EMI installments

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS mandate_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emi_tenure_months INTEGER;

CREATE INDEX IF NOT EXISTS idx_payments_mandate_reference ON payments(mandate_reference);
