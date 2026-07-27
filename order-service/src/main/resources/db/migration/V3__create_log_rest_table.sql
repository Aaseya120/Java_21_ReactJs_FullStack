-- V3: Create log_rest table for tracking industry standard REST audit logs

CREATE TABLE IF NOT EXISTS log_rest (
    id            BIGSERIAL PRIMARY KEY,
    user_id       VARCHAR(100)  NOT NULL,
    timestamp     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    http_method   VARCHAR(10)   NOT NULL,
    request_url   VARCHAR(1000) NOT NULL,
    request_body  TEXT,
    response_body TEXT,
    status_code   INTEGER       NOT NULL,
    error_code    VARCHAR(100),
    error_desc    TEXT,
    request_id    VARCHAR(100)  NOT NULL,
    service_name  VARCHAR(100)  NOT NULL,
    client_ip     VARCHAR(100),
    user_agent    VARCHAR(500),
    duration_ms   BIGINT        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_log_rest_user_id    ON log_rest(user_id);
CREATE INDEX IF NOT EXISTS idx_log_rest_request_id ON log_rest(request_id);
CREATE INDEX IF NOT EXISTS idx_log_rest_timestamp  ON log_rest(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_rest_status     ON log_rest(status_code);
