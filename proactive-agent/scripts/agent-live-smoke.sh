#!/usr/bin/env bash
# End to end, against the real Claude Managed Agents: starts the Event Inbox and the agent service from
# their jars, then checks that an application gets a conversation answered, an event of its systems
# turned into a notification, and a question of the agent answered by the user.
#
#   export ANTHROPIC_API_KEY=...          # a key with access to Managed Agents
#   mvn install -DskipTests               # once, to build the jars
#   scripts/agent-live-smoke.sh [--demo] [--model claude-opus-5-5] [--timeout-seconds 180]
#
# --demo runs the agent service in demo mode instead, without a key, to try the script itself.
# It uses the local databases inbox_dev and agent_dev (scripts/pg-local.sh), ports 18080 and 18090,
# and a new application each time. Sessions it makes on the platform are not deleted.
#
# To check a service that runs already, for example the one of docker-compose.yml, name it and its
# administration key; the script then starts nothing and needs no jars, and the service's own
# configuration decides between live and demo mode (add --demo when it runs in demo mode):
#
#   scripts/agent-live-smoke.sh --api http://localhost:8090 --admin-key <AGENT_ADMIN_KEYS_0>
# Exit code 0: every check passed. 1: a check failed. 2: could not run.
set -uo pipefail
cd "$(dirname "$0")/.."

MODE=live MODEL=claude-opus-5-5 WAIT=180 API="" ADMIN=""
while [ $# -gt 0 ]; do
  case $1 in
    --demo) MODE=demo ;;
    --model) MODEL=$2; shift ;;
    --timeout-seconds) WAIT=$2; shift ;;
    --api) API=$2; shift ;;
    --admin-key) ADMIN=$2; shift ;;
    *) echo "unknown option $1" >&2; exit 2 ;;
  esac
  shift
done
cleanup() { kill "${INBOX_PID:-}" "${AGENT_PID:-}" 2>/dev/null; wait 2>/dev/null; }
trap cleanup EXIT

if [ -n "$API" ]; then
  API=${API%/}
  [ -n "$ADMIN" ] || { echo "--api needs --admin-key, the administration key of that service" >&2; exit 2; }
  curl -sf -o /dev/null "$API/actuator/health" || { echo "no agent service answers at $API" >&2; exit 2; }
else
  if [ "$MODE" = live ] && [ -z "${ANTHROPIC_API_KEY:-}" ]; then
    echo "Set ANTHROPIC_API_KEY to a key with access to Claude Managed Agents, or use --demo." >&2
    exit 2
  fi
  for jar in inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar agent-server/target/agent-server-0.1.0-SNAPSHOT-exec.jar; do
    [ -f "$jar" ] || { echo "$jar is missing: run mvn install -DskipTests first" >&2; exit 2; }
  done

  LOGS=$(mktemp -d)
  ADMIN=smoke-admin-key-0123456789abcdef0123456789
  API=http://127.0.0.1:18090
  INBOX_PORT=18080 INBOX_AUTH_CLIENTS_0_ID=agent-server INBOX_AUTH_CLIENTS_0_SECRET=smoke-inbox-secret \
    INBOX_AUTH_CLIENTS_0_ROLES=RUNTIME,PRODUCER INBOX_RETRY_MAXATTEMPTS=10 \
    java -jar inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar > "$LOGS/inbox.log" 2>&1 &
  INBOX_PID=$!
  AGENT_PORT=18090 AGENT_INBOX_URL=http://127.0.0.1:18080 AGENT_INBOX_SECRET=smoke-inbox-secret \
    AGENT_ADMIN_KEYS_0=$ADMIN AGENT_TOKENS_SECRETS_0=smoke-token-secret-0123456789abcdef0123 AGENT_CMA_MODE=$MODE \
    java -jar agent-server/target/agent-server-0.1.0-SNAPSHOT-exec.jar > "$LOGS/agent.log" 2>&1 &
  AGENT_PID=$!
  for _ in $(seq 1 90); do
    curl -sf -o /dev/null http://127.0.0.1:18080/actuator/health && curl -sf -o /dev/null $API/actuator/health && break
    sleep 1
  done
  curl -sf -o /dev/null $API/actuator/health || { echo "the services did not start; logs in $LOGS" >&2; exit 2; }
fi

FAILED=0
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; FAILED=1; }
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
call() { # method path body credential
  if [ -n "${3:-}" ]; then
    curl -sS -X "$1" "$API$2" -H "Authorization: Bearer $4" -H 'Content-Type: application/json' -d "$3"
  else
    curl -sS -X "$1" "$API$2" -H "Authorization: Bearer $4"
  fi
}
# waits until the python expression over the updates page is not empty; prints it
await_updates() {
  local expression=$1 found=""
  for _ in $(seq 1 "$WAIT"); do
    found=$(call GET "/v1/users/u1/updates?after=0&limit=500" "" "$KEY" | json "$expression")
    [ -n "$found" ] && break
    sleep 1
  done
  echo "$found"
}

APP=smoke-$(date +%s)
call POST /v1/admin/apps "{\"app_id\": \"$APP\", \"name\": \"Smoke test\"}" "$ADMIN" > /dev/null
KEY=$(call POST /v1/admin/apps/$APP/keys '{"label": "smoke"}' "$ADMIN" | json "d['api_key']")
PROFILE=$(python3 -c "import json; print(json.dumps({'name': 'Smoke', 'model': '$MODEL', 'instructions':
  'You are the assistant of a test of this service. Answer in one short sentence. '
  'When the user asks you to book something, first ask them for the time with ask_user, then confirm the booking with that time. '
  'When an order is delayed, tell the user with notify_user, and mention the order number.'}))")
if [ -z "$KEY" ]; then echo "could not open an application: is the administration key right?" >&2; exit 2; fi
if call PUT /v1/agents/default "$PROFILE" "$KEY" | grep -q '"agent_key":"default"'; then pass "the agent is set up ($MODE, $MODEL)"; else fail "the agent is set up"; fi

# 1. A conversation.
call POST /v1/users/u1/messages '{"text": "Reply with the single word pong."}' "$KEY" > /dev/null
REPLY=$(await_updates "next((u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='reply'), '')")
if echo "$REPLY" | grep -qi pong; then pass "a message is answered: $REPLY"; else fail "a message is answered (got: ${REPLY:-nothing})"; fi

# 2. An event of the application's systems.
call POST /v1/users/u1/events '{"type": "order.delayed", "subject": "order-1234", "data": {"order_id": "1234", "new_delivery_date": "2026-11-27"}}' "$KEY" > /dev/null
NOTE=$(await_updates "next((u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='notification'), '')")
if echo "$NOTE" | grep -q 1234; then pass "an event becomes a notification: $NOTE"; else fail "an event becomes a notification (got: ${NOTE:-nothing})"; fi

# 3. A question of the agent, answered by the user.
if [ "$MODE" = demo ]; then
  echo "SKIP  a question of the agent: the demo agent asks none"
else
  call POST /v1/users/u1/messages '{"conversation_id": "booking", "text": "Please book a table for two tonight."}' "$KEY" > /dev/null
  ACTION=$(await_updates "next((u['data']['action_id'] for u in d['updates'] if u['type']=='action.created' and u['data']['kind']=='question'), '')")
  if [ -n "$ACTION" ]; then
    pass "the agent asks: $(call GET /v1/users/u1/actions/$ACTION "" "$KEY" | json "d['question']")"
    call POST /v1/users/u1/actions/$ACTION/resolve '{"answer": "7 pm"}' "$KEY" > /dev/null
    CONFIRMED=$(await_updates "next((u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='reply' and u['data']['conversation_id']=='booking'), '')")
    if [ -n "$CONFIRMED" ]; then pass "the answer reaches the agent, which goes on: $CONFIRMED"; else fail "the answer reaches the agent"; fi
  else
    fail "the agent asks a question"
  fi
fi

[ -n "${LOGS:-}" ] && echo "logs: $LOGS"
exit $FAILED
