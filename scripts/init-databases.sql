-- Initialize databases for Mercury Order System

-- Create databases
CREATE DATABASE orders_db;
CREATE DATABASE orders_dev_db;
CREATE DATABASE payments_db;
CREATE DATABASE inventory_db;
CREATE DATABASE gateway_db;

-- Create users
CREATE USER orders_user WITH PASSWORD 'orders_pass';
CREATE USER payments_user WITH PASSWORD 'payments_pass';
CREATE USER inventory_user WITH PASSWORD 'inventory_pass';
CREATE USER gateway_user WITH PASSWORD 'gateway_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE orders_db TO orders_user;
GRANT ALL PRIVILEGES ON DATABASE orders_dev_db TO orders_user;
GRANT ALL PRIVILEGES ON DATABASE payments_db TO payments_user;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO inventory_user;
GRANT ALL PRIVILEGES ON DATABASE gateway_db TO gateway_user;

-- Connect to each database and create schemas
\c orders_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c orders_dev_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c payments_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c inventory_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c gateway_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";












