CREATE TABLE IF NOT EXISTS outbox_events (
    id           BIGSERIAL PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    processed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_unprocessed ON outbox_events(created_at ASC) WHERE processed = FALSE;

