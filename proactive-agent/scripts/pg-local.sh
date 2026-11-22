#!/usr/bin/env bash
# Starts a locally installed PostgreSQL cluster (Debian/Ubuntu packaging) and creates the
# roles and databases used for development and for the test suite. Safe to run repeatedly.
# The Event Inbox and the agent service each get a role and databases of their own.
#
#   INBOX_DB_USER / INBOX_DB_PASSWORD   role of the Inbox           (default: inbox / inbox)
#   AGENT_DB_USER / AGENT_DB_PASSWORD   role of the agent service   (default: agent / agent)
#   PG_VERSION / PG_CLUSTER             cluster to start            (default: 16 / main)
#
# These are throwaway local credentials. Never reuse them outside a developer machine.
set -euo pipefail

PG_VERSION="${PG_VERSION:-16}"
PG_CLUSTER="${PG_CLUSTER:-main}"
# role:password:database,database
SERVICES=(
  "${INBOX_DB_USER:-inbox}:${INBOX_DB_PASSWORD:-inbox}:inbox_dev,inbox_test"
  "${AGENT_DB_USER:-agent}:${AGENT_DB_PASSWORD:-agent}:agent_dev,agent_test"
)

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

for service in "${SERVICES[@]}"; do
  IFS=: read -r user pass databases <<<"$service"
  admin_psql -v user="$user" -v pass="$pass" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'user', :'pass')
 WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'user') \gexec
SQL
  for db in ${databases//,/ }; do
    admin_psql -v db="$db" -v user="$user" <<'SQL'
SELECT format('CREATE DATABASE %I OWNER %I', :'db', :'user')
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db') \gexec
SQL
  done
  echo "PostgreSQL is ready: role '$user', databases: ${databases//,/ }"
done
echo "  Inbox dev : jdbc:postgresql://127.0.0.1:5432/inbox_dev    test: .../inbox_test"
echo "  Agent dev : jdbc:postgresql://127.0.0.1:5432/agent_dev    test: .../agent_test"
