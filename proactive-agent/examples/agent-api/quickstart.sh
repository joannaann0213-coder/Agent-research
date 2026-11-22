#!/usr/bin/env bash
# The quick start of the Agent API guide (docs/en/agent-api.md), step by step, against a running
# agent service. Works in demo mode, where a script plays the agent.
#
#   API=http://localhost:8090 ADMIN=<an administration key> examples/agent-api/quickstart.sh
#
# Needs curl and python3 (to read JSON).
set -euo pipefail

API=${API:-http://localhost:8090}
ADMIN=${ADMIN:-change-me-admin-key-0123456789abcdef}
APP=${APP:-acme}
USER_ID=${USER_ID:-u42}

json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }
call() { # method path [body] [credential]
  local method=$1 path=$2 body=${3:-} credential=${4:-$KEY}
  if [[ -n $body ]]; then
    curl -sS -X "$method" "$API$path" -H "Authorization: Bearer $credential" -H 'Content-Type: application/json' -d "$body"
  else
    curl -sS -X "$method" "$API$path" -H "Authorization: Bearer $credential"
  fi
}

echo "1. The operator registers the application (a 409 means it exists already)."
call POST /v1/admin/apps "{\"app_id\": \"$APP\", \"name\": \"Acme\"}" "$ADMIN"; echo

echo "2. ... and gives its back end an API key, shown this once."
KEY=$(call POST /v1/admin/apps/$APP/keys '{"label": "quickstart"}' "$ADMIN" | json "['api_key']")
echo "   ${KEY:0:16}..."

echo "3. The application describes its agent."
call PUT /v1/agents/default '{"name": "Assistant", "instructions": "You help Acme customers with their orders."}'; echo

echo "4. The back end gets a token for the user's own client."
TOKEN=$(call POST /v1/users/$USER_ID/tokens '' | json "['token']")

echo "5. The user writes, with their token. 202: stored, not answered yet."
SEQ=$(call POST /v1/users/$USER_ID/messages '{"text": "Where is my order 1234?"}' "$TOKEN" | json "['seq']")
echo "   message at seq $SEQ"

echo "6. The reply comes a moment later."
for _ in $(seq 1 30); do
  REPLY=$(call GET "/v1/users/$USER_ID/messages?after=$SEQ" '' "$TOKEN" \
    | python3 -c "import json,sys; m=[x for x in json.load(sys.stdin)['messages'] if x['kind']=='reply']; print(m[0]['text'] if m else '')")
  [[ -n $REPLY ]] && break
  sleep 1
done
echo "   agent: ${REPLY:-(no reply within 30 seconds)}"

echo "7. Something happens in the application's systems."
call POST /v1/users/$USER_ID/events '{"type": "order.delayed", "subject": "order-1234", "data": {"order_id": "1234", "new_delivery_date": "2026-11-27"}}'; echo

echo "8. The agent decides whether the user needs to know; what it says shows among the updates."
for _ in $(seq 1 30); do
  NOTE=$(call GET "/v1/users/$USER_ID/updates?after=0" '' "$TOKEN" \
    | python3 -c "import json,sys; u=[x for x in json.load(sys.stdin)['updates'] if x['type']=='message.created' and x['data']['kind']=='notification']; print(u[-1]['data']['text'] if u else '')")
  [[ -n $NOTE ]] && break
  sleep 1
done
echo "   notification: ${NOTE:-(none within 30 seconds)}"

echo "9. Everything that happened for the user, in order:"
call GET "/v1/users/$USER_ID/updates?after=0" '' "$TOKEN" \
  | python3 -c "import json,sys; [print('  ', x['seq'], x['type'], x['data'].get('kind', x['data'].get('status', ''))) for x in json.load(sys.stdin)['updates']]"
