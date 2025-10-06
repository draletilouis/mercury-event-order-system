-- Test data for Orders Service
-- Version 2: Insert sample data for testing

-- Insert sample orders (only in test profile)
INSERT INTO orders (id, customer_id, status, total_amount, currency, created_at, updated_at, version)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'customer-001', 'COMPLETED', 99.99, 'USD', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 0),
    ('550e8400-e29b-41d4-a716-446655440002', 'customer-002', 'PAYMENT_PENDING', 149.50, 'USD', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', 0),
    ('550e8400-e29b-41d4-a716-446655440003', 'customer-001', 'CANCELLED', 75.00, 'USD', NOW() - INTERVAL '3 hours', NOW() - INTERVAL '2 hours', 0);

-- Insert sample order items
INSERT INTO order_items (id, order_id, sku, quantity, unit_price, created_at)
VALUES 
    -- Order 1 items
    ('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 'LAPTOP-001', 1, 99.99, NOW() - INTERVAL '1 day'),
    
    -- Order 2 items
    ('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'MOUSE-001', 2, 25.00, NOW() - INTERVAL '2 hours'),
    ('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440002', 'KEYBOARD-001', 1, 99.50, NOW() - INTERVAL '2 hours'),
    
    -- Order 3 items
    ('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440003', 'HEADPHONES-001', 1, 75.00, NOW() - INTERVAL '3 hours');

-- Note: This data is only inserted when running with test profile
-- In production, this migration will be skipped or the data will be conditionally inserted

