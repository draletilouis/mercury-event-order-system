-- Inventory baseline schema

CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY,
    sku VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    unit_cost NUMERIC(19,2),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inventory_items_sku ON inventory_items (sku);

CREATE TABLE IF NOT EXISTS inventory_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    sku VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_inventory_reservations_order ON inventory_reservations (order_id);
CREATE INDEX IF NOT EXISTS idx_inventory_reservations_sku ON inventory_reservations (sku);
CREATE INDEX IF NOT EXISTS idx_inventory_reservations_status_expires ON inventory_reservations (status, expires_at);


