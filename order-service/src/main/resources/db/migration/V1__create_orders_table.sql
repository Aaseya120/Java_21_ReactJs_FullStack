-- V1: Create orders table and its updated_at trigger function

-- Create the trigger function if it doesn't exist yet
-- (may already exist from user-service migration)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS orders (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    quantity     INT          NOT NULL CHECK (quantity > 0),
    total_price  NUMERIC(12, 2) NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    notes        TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id  ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status   ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created  ON orders(created_at DESC);

DROP TRIGGER IF EXISTS trg_orders_updated_at ON orders;
CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

