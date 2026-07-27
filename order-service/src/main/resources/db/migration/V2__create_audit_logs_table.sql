-- V2: Create audit_logs table for tracking all user actions

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    service_name    VARCHAR(100)   NOT NULL,
    action          VARCHAR(100)   NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(255),
    user_id         VARCHAR(255),
    user_ip         VARCHAR(50),
    http_method     VARCHAR(10),
    request_uri     TEXT,
    request_body    TEXT,
    response_status INT,
    duration_ms     BIGINT,
    details         TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_service    ON audit_logs(service_name);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action     ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);

