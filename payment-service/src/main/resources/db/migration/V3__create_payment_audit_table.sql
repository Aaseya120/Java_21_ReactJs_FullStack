-- V3: Create payment_audit_logs table for financial audit trails

CREATE TABLE IF NOT EXISTS payment_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    payment_id      VARCHAR(36) NOT NULL,
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    reason          VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_audit_payment_id ON payment_audit_logs(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_audit_created_at ON payment_audit_logs(created_at DESC);
