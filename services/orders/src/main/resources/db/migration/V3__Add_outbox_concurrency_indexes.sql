-- Migration to add better indexes for concurrent outbox event processing
-- Version 3: Improve concurrency handling for outbox pattern

-- Add composite index for efficient querying of pending events by status and creation time
-- This will help with the findByStatusOrderByCreatedAtAsc query performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_outbox_events_status_created_at 
ON outbox_events(status, created_at);

-- Add index for failed events retry queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_outbox_events_failed_retry 
ON outbox_events(status, retry_count, created_at) 
WHERE status = 'FAILED';

-- Add index for cleanup of old published events
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_outbox_events_cleanup 
ON outbox_events(status, published_at) 
WHERE status = 'PUBLISHED';

-- Add partial index for pending events only (most frequently queried)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_outbox_events_pending_only 
ON outbox_events(created_at) 
WHERE status = 'PENDING';

-- Add aggregate_id index for event correlation and debugging
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_outbox_events_aggregate_id 
ON outbox_events(aggregate_id);

-- Remove old less efficient indexes that are now redundant
DROP INDEX IF EXISTS idx_outbox_events_status;
DROP INDEX IF EXISTS idx_outbox_events_created_at;























