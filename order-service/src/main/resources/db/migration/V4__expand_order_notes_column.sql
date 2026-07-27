-- ============================================================================
-- Flyway Migration: V4__expand_order_notes_column.sql
-- Pattern: Expand & Contract (Parallel Change) for Zero-Downtime Deployment
-- ============================================================================
-- PHASE 1: EXPAND
-- Add the new column as NULLABLE so older running application pods (Blue)
-- can continue inserting rows without failing on NOT NULL constraints while
-- newer pods (Green) begin reading and writing to this expanded column.
-- Never drop or rename existing columns in a single release.
-- ============================================================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_notes VARCHAR(500);

COMMENT ON COLUMN orders.order_notes IS 'Optional customer notes added during order placement (Expand & Contract V4)';
