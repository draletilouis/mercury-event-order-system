CREATE TABLE IF NOT EXISTS order_read_model (
    order_id UUID PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    last_event_type VARCHAR(64) NOT NULL,
    last_event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_read_model_customer_status
    ON order_read_model (customer_id, status);

CREATE INDEX IF NOT EXISTS idx_order_read_model_last_event_at
    ON order_read_model (last_event_at DESC);


