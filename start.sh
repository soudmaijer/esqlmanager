#!/usr/bin/env bash
# Starts a local PostgreSQL in Docker with a small sample database and then the application.
#   ./start.sh            start the database (if needed) and the application
#   ./start.sh --db-only  only start the database
set -euo pipefail

cd "$(dirname "$0")"

CONTAINER=esql-pg
IMAGE=postgres:17
PORT=5432
PASSWORD=test

if ! docker info >/dev/null 2>&1; then
	echo "Docker is not running." >&2
	exit 1
fi

if [ "$(docker ps -q -f "name=^${CONTAINER}$")" ]; then
	echo "PostgreSQL container ${CONTAINER} is already running."
elif [ "$(docker ps -aq -f "name=^${CONTAINER}$")" ]; then
	echo "Starting the existing container ${CONTAINER}..."
	docker start "${CONTAINER}" >/dev/null
else
	echo "Creating PostgreSQL container ${CONTAINER} (${IMAGE})..."
	docker run -d --name "${CONTAINER}" -e POSTGRES_PASSWORD="${PASSWORD}" -p "${PORT}:5432" "${IMAGE}" >/dev/null
fi

echo -n "Waiting for PostgreSQL"
until docker exec "${CONTAINER}" pg_isready -U postgres -q 2>/dev/null; do
	echo -n "."
	sleep 1
done
echo " ready."

# The server restarts once while the image initialises itself, so check that a real query works.
until docker exec "${CONTAINER}" psql -U postgres -tAc "SELECT 1" >/dev/null 2>&1; do
	sleep 1
done

if [ -z "$(docker exec "${CONTAINER}" psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'shop'")" ]; then
	echo "Creating the sample database 'shop'..."
	docker exec "${CONTAINER}" psql -U postgres -q -c "CREATE DATABASE shop"
	docker exec -i "${CONTAINER}" psql -U postgres -q -d shop <<'SQL'
CREATE TABLE customers (
	id serial PRIMARY KEY,
	name varchar(100) NOT NULL,
	email varchar(150) UNIQUE
);
CREATE TABLE products (
	id serial PRIMARY KEY,
	name varchar(100) NOT NULL,
	price numeric(10, 2) NOT NULL,
	stock integer NOT NULL DEFAULT 0
);
CREATE TABLE orders (
	id serial PRIMARY KEY,
	customer_id integer NOT NULL REFERENCES customers (id),
	product_id integer NOT NULL REFERENCES products (id),
	quantity integer NOT NULL DEFAULT 1,
	ordered_at timestamp NOT NULL DEFAULT now()
);
CREATE INDEX orders_customer_idx ON orders (customer_id);
INSERT INTO customers (name, email) VALUES
	('Ada Lovelace', 'ada@example.com'),
	('Grace Hopper', 'grace@example.com'),
	('Alan Turing', 'alan@example.com');
INSERT INTO products (name, price, stock) VALUES
	('Keyboard', 49.95, 25),
	('Mouse', 19.50, 80),
	('Monitor 27"', 289.00, 12),
	('USB-C cable', 7.25, 200);
INSERT INTO orders (customer_id, product_id, quantity) VALUES
	(1, 1, 1), (1, 3, 2), (2, 2, 1), (3, 4, 5), (3, 1, 1);
SQL
fi

cat <<INFO

Connect from eSQLManager with:
  Server type  PostgreSQL
  Host         localhost
  Port         ${PORT}
  User         postgres
  Password     ${PASSWORD}
  Database(s)  shop   (optional, also the database the connection is made to)

Stop and remove the database with ./stop.sh
INFO

if [ "${1:-}" = "--db-only" ]; then
	exit 0
fi

echo
echo "Starting eSQLManager..."
exec ./mvnw -q compile exec:exec
