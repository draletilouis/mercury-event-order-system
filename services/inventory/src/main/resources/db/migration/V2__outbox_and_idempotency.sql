-- Outbox and idempotency

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    event_data TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created ON outbox_events (status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_retry ON outbox_events (status, retry_count, created_at);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    consumer VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NULL,
    PRIMARY KEY (consumer, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_keys (expires_at);


