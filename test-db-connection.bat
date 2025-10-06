@echo off
echo Testing PostgreSQL connection...

echo.
echo 1. Testing connection to orders_db:
psql -h localhost -p 5432 -U orders_user -d orders_db -c "SELECT version();"

echo.
echo 2. Testing connection to payments_db:
psql -h localhost -p 5432 -U payments_user -d payments_db -c "SELECT version();"

echo.
echo 3. Testing connection to inventory_db:
psql -h localhost -p 5432 -U inventory_user -d inventory_db -c "SELECT version();"

echo.
echo 4. Listing all databases:
psql -h localhost -p 5432 -U postgres -c "\l"

echo.
echo 5. Listing all users:
psql -h localhost -p 5432 -U postgres -c "\du"

pause
