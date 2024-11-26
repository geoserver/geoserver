#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER cite PASSWORD 'cite';
    CREATE DATABASE cite;
    GRANT ALL PRIVILEGES ON DATABASE cite TO cite;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "cite" <<-EOSQL
    CREATE EXTENSION postgis;
    GRANT ALL PRIVILEGES ON SCHEMA public TO cite;
EOSQL

