#!/usr/bin/env bash
# Starts a locally installed PostgreSQL cluster (Debian/Ubuntu packaging) and creates the
# role and databases used for development and for the test suite. Safe to run repeatedly.
#
#   INBOX_DB_USER / INBOX_DB_PASSWORD   role to create          (default: inbox / inbox)
#   PG_VERSION / PG_CLUSTER             cluster to start        (default: 16 / main)
#
# These are throwaway local credentials. Never reuse them outside a developer machine.
set -euo pipefail

PG_VERSION="${PG_VERSION:-16}"
PG_CLUSTER="${PG_CLUSTER:-main}"
DB_USER="${INBOX_DB_USER:-inbox}"
DB_PASSWORD="${INBOX_DB_PASSWORD:-inbox}"
DATABASES=("inbox_dev" "inbox_test")

if command -v pg_ctlcluster >/dev/null 2>&1; then
  status="$(pg_lsclusters -h | awk -v v="$PG_VERSION" -v c="$PG_CLUSTER" '$1==v && $2==c {print $4}')"
  if [ "$status" != "online" ]; then
    echo "Starting PostgreSQL $PG_VERSION/$PG_CLUSTER"
    pg_ctlcluster "$PG_VERSION" "$PG_CLUSTER" start
  fi
fi

# psql as the postgres superuser: directly when root, through sudo otherwise.
admin_psql() {
  if [ "$(id -u)" = "0" ]; then
    runuser -u postgres -- psql -v ON_ERROR_STOP=1 -q "$@"
  else
    sudo -u postgres psql -v ON_ERROR_STOP=1 -q "$@"
  fi
}

for _ in $(seq 1 30); do
  if admin_psql -c 'select 1' >/dev/null 2>&1; then break; fi
  sleep 1
done

admin_psql -v user="$DB_USER" -v pass="$DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'user', :'pass')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'user') \gexec
SQL

for db in "${DATABASES[@]}"; do
  admin_psql -v db="$db" -v user="$DB_USER" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER %I', :'db', :'user')
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db') \gexec
SQL
done

echo "PostgreSQL is ready: role '$DB_USER', databases: ${DATABASES[*]}"
echo "  dev : jdbc:postgresql://127.0.0.1:5432/inbox_dev"
echo "  test: jdbc:postgresql://127.0.0.1:5432/inbox_test"
