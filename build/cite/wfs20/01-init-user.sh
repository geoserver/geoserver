#!/bin/bash
set -e

DB_NAME="cite_wfs20"

psql -v ON_ERROR_STOP=0 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER cite PASSWORD 'cite';
    CREATE DATABASE $DB_NAME;
    GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO cite;
EOSQL

psql -v ON_ERROR_STOP=0 --username "$POSTGRES_USER" --dbname "$DB_NAME" <<-EOSQL
    CREATE EXTENSION postgis;
    GRANT ALL PRIVILEGES ON SCHEMA public TO cite;
EOSQL

