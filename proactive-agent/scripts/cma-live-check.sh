#!/usr/bin/env bash
# Checks, against the real service, the behaviour of Claude Managed Agents that the agent service
# depends on. Needs ANTHROPIC_API_KEY (a key with access to Managed Agents) and makes a handful of
# small model requests. Exit code 0: everything is as assumed. 1: something differs. 2: could not run.
#
#   export ANTHROPIC_API_KEY=...
#   scripts/cma-live-check.sh [--model claude-opus-5-5] [--no-sandbox] [--timeout-seconds 180]
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
  echo "Set ANTHROPIC_API_KEY to a key with access to Claude Managed Agents." >&2
  exit 2
fi

mvn -q -pl agent-cma -am install -DskipTests
CLASSPATH_FILE="$(mktemp)"
trap 'rm -f "$CLASSPATH_FILE"' EXIT
mvn -q -pl agent-cma dependency:build-classpath -Dmdep.outputFile="$CLASSPATH_FILE" -Dmdep.includeScope=runtime
exec java -cp "agent-cma/target/classes:$(cat "$CLASSPATH_FILE")" dev.agentflow.agent.cma.live.LiveCheck "$@"
