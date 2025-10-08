-- Payments service baseline schema

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(32) NOT NULL,
    payment_method VARCHAR(100),
    external_payment_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments (created_at);
CREATE INDEX IF NOT EXISTS idx_payments_external_id ON payments (external_payment_id);

CREATE TABLE IF NOT EXISTS payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    external_response TEXT,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_attempts_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_payment_id ON payment_attempts (payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_status ON payment_attempts (status);
CREATE INDEX IF NOT EXISTS idx_payment_attempts_attempted_at ON payment_attempts (attempted_at);


