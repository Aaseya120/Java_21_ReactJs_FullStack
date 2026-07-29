-- V2: Create payment_outbox_events table for Transactional Outbox Pattern

CREATE TABLE IF NOT EXISTS payment_outbox_events (
    id            BIGSERIAL PRIMARY KEY,
    aggregate_id  VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL DEFAULT 'PAYMENT',
    event_type    VARCHAR(100) NOT NULL,
    payload       TEXT NOT NULL,
    processed     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_outbox_unprocessed ON payment_outbox_events(processed, created_at ASC);
