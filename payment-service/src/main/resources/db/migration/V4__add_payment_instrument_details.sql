-- V4: Add industry-standard payment instrument metadata columns for Credit/Debit Cards, UPI, Net Banking, and Wallets

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS card_last4       VARCHAR(4),
    ADD COLUMN IF NOT EXISTS card_brand       VARCHAR(30),
    ADD COLUMN IF NOT EXISTS upi_vpa          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bank_code        VARCHAR(30),
    ADD COLUMN IF NOT EXISTS wallet_provider  VARCHAR(50),
    ADD COLUMN IF NOT EXISTS gateway_provider VARCHAR(50) DEFAULT 'SIMULATED_GATEWAY';

CREATE INDEX IF NOT EXISTS idx_payments_upi_vpa     ON payments(upi_vpa);
CREATE INDEX IF NOT EXISTS idx_payments_bank_code   ON payments(bank_code);
