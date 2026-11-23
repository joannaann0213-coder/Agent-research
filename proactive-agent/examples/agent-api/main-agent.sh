#!/usr/bin/env bash
# A main agent and a worker (docs/en/main-agent.md), step by step, against a running agent service.
# Works in demo mode, where a script plays both: the main agent hands on whatever the user writes
# after "task:", and the worker answers that it is done.
#
#   API=http://localhost:8090 ADMIN=<an administration key> examples/agent-api/main-agent.sh
#
# Needs curl and python3 (to read JSON).
set -euo pipefail

API=${API:-http://localhost:8090}
ADMIN=${ADMIN:-change-me-admin-key-0123456789abcdef}
APP=${APP:-acme-main}
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
notifications() { # how many notifications the user has got so far
  call GET "/v1/users/$USER_ID/updates?after=0" '' "$TOKEN" \
    | python3 -c "import json,sys; print(len([x for x in json.load(sys.stdin)['updates'] if x['type']=='message.created' and x['data']['kind']=='notification']))"
}
last_notification() {
  call GET "/v1/users/$USER_ID/updates?after=0" '' "$TOKEN" \
    | python3 -c "import json,sys; u=[x for x in json.load(sys.stdin)['updates'] if x['type']=='message.created' and x['data']['kind']=='notification']; print(u[-1]['data']['text'] if u else '')"
}
wait_for_notification() { # the count before; prints the notification that came after it
  local before=$1
  for _ in $(seq 1 30); do
    if [[ $(notifications) -gt $before ]]; then last_notification; return; fi
    sleep 1
  done
  echo "(none within 30 seconds)"
}

echo "1. The operator registers the application (a 409 means it exists already)."
call POST /v1/admin/apps "{\"app_id\": \"$APP\", \"name\": \"Acme\"}" "$ADMIN"; echo

echo "2. ... and gives its back end an API key, shown this once."
KEY=$(call POST /v1/admin/apps/$APP/keys '{"label": "main-agent"}' "$ADMIN" | json "['api_key']")
echo "   ${KEY:0:16}..."

echo "3. A worker is an ordinary agent."
call PUT /v1/agents/researcher '{"name": "Researcher", "instructions": "You look things up in the order system and report what you found."}'; echo

echo "4. The main agent names the workers it may hand work to."
call PUT /v1/agents/default '{"name": "Assistant", "instructions": "You look after Acme customers.", "coordinator": {"workers": [{"role": "research", "agent": "researcher", "description": "Looks things up in the order system and reports what it found."}]}}'; echo

echo "5. The back end gets a token for the user's own client."
TOKEN=$(call POST /v1/users/$USER_ID/tokens '' | json "['token']")

echo "6. The user asks for something that takes a while."
BEFORE=$(notifications)
SEQ=$(call POST /v1/users/$USER_ID/messages '{"text": "task: find out why order 1234 is late"}' "$TOKEN" | json "['seq']")

echo "7. The main agent answers at once: it has handed the work to a worker."
for _ in $(seq 1 30); do
  REPLY=$(call GET "/v1/users/$USER_ID/messages?after=$SEQ" '' "$TOKEN" \
    | python3 -c "import json,sys; m=[x for x in json.load(sys.stdin)['messages'] if x['kind']=='reply']; print(m[0]['text'] if m else '')")
  [[ -n $REPLY ]] && break
  sleep 1
done
echo "   agent: ${REPLY:-(no reply within 30 seconds)}"

echo "8. The task, as the user's own client sees it."
call GET "/v1/users/$USER_ID/tasks?status=all" '' "$TOKEN" \
  | python3 -c "import json,sys; [print('  ', x['task_id'], x['role'], x['status'], '-', x['title']) for x in json.load(sys.stdin)['tasks']]"

echo "9. The worker reports to the main agent, and the main agent tells the user."
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "10. Something happens in the application's systems. It reaches the same main agent, in the same session."
BEFORE=$(notifications)
call POST /v1/users/$USER_ID/events '{"type": "order.delayed", "subject": "order-1234", "data": {"order_id": "1234", "new_delivery_date": "2026-11-27"}}'; echo
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "11. With the API key, the task shows what the two agents told each other."
TASK=$(call GET "/v1/users/$USER_ID/tasks?status=all" | json "['tasks'][0]['task_id']")
call GET "/v1/users/$USER_ID/tasks/$TASK" \
  | python3 -c "import json,sys; [print('  ', n['seq'], n['from'], n['kind'] + ':', n['text']) for n in json.load(sys.stdin)['notes']]"

echo "12. Everything that happened for the user, in order:"
call GET "/v1/users/$USER_ID/updates?after=0" '' "$TOKEN" \
  | python3 -c "import json,sys; [print('  ', x['seq'], x['type'], x['data'].get('kind', x['data'].get('status', ''))) for x in json.load(sys.stdin)['updates']]"
