#!/usr/bin/env bash
# Runs the scripted worker that stands in for an agent runtime: it claims runs, answers each with one
# message written to a directory (the stub outbox), and acks.
#
#   scripts/run-worker.sh <client-id> <secret> [extra ScriptedWorkerMain options]
#
# Useful options:  --max-runs N   --wait-ms MILLIS   --behaviour reply|reply-then-hang
# INBOX_URL selects the server (default http://127.0.0.1:8080); OUTBOX_DIR the outbox directory.
# Build and install the modules first:  mvn install   (add -DskipTests to skip the tests)
set -euo pipefail

if [ "$#" -lt 2 ]; then
  sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
  exit 2
fi
client_id="$1"; secret="$2"; shift 2

root="$(cd "$(dirname "$0")/.." && pwd)"
classes="$root/inbox-client/target/classes"
classpath_file="$root/inbox-client/target/worker.classpath"
hint="build and install the modules first: (cd '$root' && mvn install)"

if [ ! -d "$classes" ]; then
  echo "run-worker.sh: $classes does not exist; $hint" >&2
  exit 1
fi
if [ ! -f "$classpath_file" ]; then
  # The client's dependencies, inbox-api among them, are resolved from the local Maven repository.
  if ! mvn -q -f "$root/pom.xml" -pl inbox-client dependency:build-classpath \
      -Dmdep.outputFile="$classpath_file" >&2; then
    rm -f "$classpath_file"
    echo "run-worker.sh: could not resolve the worker's classpath; $hint" >&2
    exit 1
  fi
fi

exec java -cp "$classes:$(cat "$classpath_file")" \
  dev.agentflow.inbox.client.ScriptedWorkerMain \
  --base-url "${INBOX_URL:-http://127.0.0.1:8080}" \
  --client-id "$client_id" --secret "$secret" \
  --outbox-dir "${OUTBOX_DIR:-$root/target/outbox}" "$@"
