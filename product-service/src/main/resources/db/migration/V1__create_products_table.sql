-- V1: Create products table

-- Create the trigger function if it doesn't exist yet
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS products (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)   NOT NULL,
    description  TEXT,
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock_qty    INT            NOT NULL DEFAULT 0 CHECK (stock_qty >= 0),
    category     VARCHAR(100),
    sku          VARCHAR(100)   UNIQUE,
    image_url    TEXT,
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_active   ON products(active);
CREATE INDEX IF NOT EXISTS idx_products_sku      ON products(sku);

DROP TRIGGER IF EXISTS trg_products_updated_at ON products;
CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed sample products
INSERT INTO products (name, description, price, stock_qty, category, sku)
VALUES
  ('Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 50, 'Electronics', 'LAP-001'),
  ('Wireless Mouse',  'Ergonomic wireless mouse', 29.99, 200, 'Accessories', 'MOU-001'),
  ('USB-C Hub',  '7-in-1 USB-C hub', 49.99, 150, 'Accessories', 'HUB-001')
ON CONFLICT (sku) DO NOTHING;

