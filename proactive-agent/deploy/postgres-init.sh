#!/bin/sh
# Runs once, when the PostgreSQL container starts on an empty data directory: one database and one
# role for each service. The passwords come from the environment of the container (docker-compose.yml).
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v inbox_password="$INBOX_DB_PASSWORD" -v agent_password="$AGENT_DB_PASSWORD" <<'SQL'
CREATE ROLE inbox LOGIN PASSWORD :'inbox_password';
CREATE DATABASE inbox OWNER inbox;
CREATE ROLE agent LOGIN PASSWORD :'agent_password';
CREATE DATABASE agent OWNER agent;
SQL
