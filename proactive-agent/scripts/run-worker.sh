#!/usr/bin/env bash
# Runs the scripted worker that stands in for an agent runtime: it claims runs, answers each with one
# message written to a directory (the stub outbox), and acks.
#
#   scripts/run-worker.sh <client-id> <secret> [extra ScriptedWorkerMain options]
#
# Useful options:  --max-runs N   --wait-ms MILLIS   --behaviour reply|reply-then-hang
# INBOX_URL selects the server (default http://127.0.0.1:8080); OUTBOX_DIR the outbox directory.
# Build first with:  mvn -q -DskipTests -DskipITs install
set -euo pipefail

if [ "$#" -lt 2 ]; then
  sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
fi
client_id="$1"; secret="$2"; shift 2

root="$(cd "$(dirname "$0")/.." && pwd)"
classpath_file="$root/inbox-client/target/worker.classpath"
if [ ! -f "$classpath_file" ]; then
  mvn -q -f "$root/pom.xml" -pl inbox-client dependency:build-classpath -Dmdep.outputFile="$classpath_file" >/dev/null
fi

exec java -cp "$root/inbox-client/target/classes:$(cat "$classpath_file")" \
  dev.agentflow.inbox.client.ScriptedWorkerMain \
  --base-url "${INBOX_URL:-http://127.0.0.1:8080}" \
  --client-id "$client_id" --secret "$secret" \
  --outbox-dir "${OUTBOX_DIR:-$root/target/outbox}" "$@"
