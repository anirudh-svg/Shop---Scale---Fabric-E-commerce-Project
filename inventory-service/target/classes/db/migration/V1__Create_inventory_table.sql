-- Create inventory_items table
CREATE TABLE inventory_items (
    id BIGSERIAL PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL UNIQUE,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_available_quantity CHECK (available_quantity >= 0),
    CONSTRAINT check_reserved_quantity CHECK (reserved_quantity >= 0)
);

-- Create index on product_id for faster lookups
CREATE INDEX idx_inventory_product_id ON inventory_items(product_id);

-- Create index on last_updated for audit queries
CREATE INDEX idx_inventory_last_updated ON inventory_items(last_updated);
