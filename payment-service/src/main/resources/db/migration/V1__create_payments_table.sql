-- V1: Create payments table and updated_at trigger

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS payments (
    id                    BIGSERIAL PRIMARY KEY,
    order_id              BIGINT NOT NULL,
    user_id               BIGINT NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency              VARCHAR(10) NOT NULL DEFAULT 'USD',
    status                VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_method        VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100),
    idempotency_key       VARCHAR(100) NOT NULL UNIQUE,
    error_message         TEXT,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id        ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_id         ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status          ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency_key ON payments(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_payments_created_at      ON payments(created_at DESC);

DROP TRIGGER IF EXISTS trg_payments_updated_at ON payments;
CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
