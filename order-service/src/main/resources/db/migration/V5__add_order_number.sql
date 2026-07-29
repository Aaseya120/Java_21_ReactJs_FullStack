-- Add order_number column
ALTER TABLE orders ADD COLUMN order_number VARCHAR(50);

-- Populate order_number for existing records with a default legacy pattern
UPDATE orders SET order_number = 'ORD-LEGACY-' || id WHERE order_number IS NULL;

-- Enforce NOT NULL constraint
ALTER TABLE orders ALTER COLUMN order_number SET NOT NULL;
