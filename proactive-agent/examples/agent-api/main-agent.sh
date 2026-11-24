#!/usr/bin/env bash
# A main agent and a worker (docs/en/main-agent.md), step by step, against a running agent service.
# Works in demo mode, where a script plays both: the main agent hands on whatever the user writes
# after "task:", and the worker answers that it is done. An event whose data has a text "task" makes
# the main agent take that work up by itself: it then waits for the user's confirmation, unless the
# user has allowed the role to start unasked. An event that the application sends to the worker itself
# becomes a task of the main agent, which hears of it at once; so does what the application schedules
# for the worker, when it is due. A task whose words say "needs approval" makes the worker call its
# tool server, which waits for the user: the main agent is told at once, and tells the user. And
# "proactivity:" with a level tells the main agent how much the user wants to hear from it.
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

echo "3. A worker is an ordinary agent. This one has a tool server whose calls the user has to approve."
call PUT /v1/agents/researcher '{"name": "Researcher", "instructions": "You look things up in the order system and report what you found.", "mcp_servers": [{"name": "orders", "url": "https://mcp.example.com/orders", "approval": "always_ask", "approver": "user"}]}'; echo

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

echo "11. Something happens that the main agent could act on by itself. It proposes the work, and waits for the user."
call POST /v1/users/$USER_ID/events '{"type": "order.delayed", "subject": "order-1234", "data": {"task": "find a new delivery slot for order 1234"}}'; echo
PROPOSED=""
for _ in $(seq 1 30); do
  PROPOSED=$(call GET "/v1/users/$USER_ID/tasks?status=proposed" '' "$TOKEN" \
    | python3 -c "import json,sys; t=json.load(sys.stdin)['tasks']; print(t[0]['task_id'] if t else '')")
  [[ -n $PROPOSED ]] && break
  sleep 1
done
[[ -n $PROPOSED ]] || { echo "   (no task to confirm within 30 seconds)"; exit 1; }
call GET "/v1/users/$USER_ID/tasks?status=proposed" '' "$TOKEN" \
  | python3 -c "import json,sys; [print('  ', x['task_id'], x['status'], '-', x['summary']) for x in json.load(sys.stdin)['tasks']]"

echo "12. The user confirms it in their own client, and the worker starts."
BEFORE=$(notifications)
echo "   confirmed by: $(call POST "/v1/users/$USER_ID/tasks/$PROPOSED/start" '' "$TOKEN" | json "['authorized_by']")"
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "13. The user allows the main agent to start tasks of this role by itself from now on."
call PUT "/v1/users/$USER_ID/grants/default/research" '' "$TOKEN"; echo

echo "14. The next time, the work starts without a question."
BEFORE=$(notifications)
call POST /v1/users/$USER_ID/events '{"type": "order.delayed", "subject": "order-5678", "data": {"task": "find a new delivery slot for order 5678"}}'; echo
echo "   notification: $(wait_for_notification "$BEFORE")"
call GET "/v1/users/$USER_ID/tasks?status=all" \
  | python3 -c "import json,sys; [print('  ', x['task_id'], x['status'], 'authorized by', x.get('authorized_by', '-'), '-', x['title']) for x in json.load(sys.stdin)['tasks']]"

echo "15. The user takes the authorisation back."
call DELETE "/v1/users/$USER_ID/grants/default/research" '' "$TOKEN"
call GET "/v1/users/$USER_ID/grants" '' "$TOKEN"; echo

echo "16. The application hands an event straight to the worker. It becomes a task of the main agent, which hears of it at once and passes on what the worker found."
BEFORE=$(notifications)
call POST /v1/users/$USER_ID/events '{"type": "order.returned", "subject": "order-7777", "agent": "researcher", "data": {"order_id": "7777"}}'; echo
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "17. The application schedules something for the worker, a few seconds from now. When it is due it goes the same way, to the task of its subject."
AT=$(python3 -c "import datetime; print((datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(seconds=4)).strftime('%Y-%m-%dT%H:%M:%SZ'))")
BEFORE=$(notifications)
call POST /v1/users/$USER_ID/schedules "{\"name\": \"Returns check\", \"instructions\": \"See whether order 7777 has come back.\", \"at\": \"$AT\", \"agent\": \"researcher\", \"subject\": \"order-7777\"}"; echo
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "18. The user asks for something that the worker may not do without them. The worker waits; the main agent is told at once, and tells the user."
BEFORE=$(notifications)
call POST /v1/users/$USER_ID/messages '{"text": "task: refund order 7777, needs approval"}' "$TOKEN" > /dev/null
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "19. The user approves it in their own client, and the worker goes on."
ACTION=$(call GET "/v1/users/$USER_ID/actions" '' "$TOKEN" | json "['actions'][0]['action_id']")
BEFORE=$(notifications)
echo "   the action is now: $(call POST "/v1/users/$USER_ID/actions/$ACTION/resolve" '{"decision": "approve"}' "$TOKEN" | json "['status']")"
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "20. The user finds that too much, and tells the main agent to be quiet. It is on record, and the user's client can read and set it too."
SEQ=$(call POST /v1/users/$USER_ID/messages '{"text": "proactivity: quiet"}' "$TOKEN" | json "['seq']")
for _ in $(seq 1 30); do
  REPLY=$(call GET "/v1/users/$USER_ID/messages?after=$SEQ" '' "$TOKEN" \
    | python3 -c "import json,sys; m=[x for x in json.load(sys.stdin)['messages'] if x['kind']=='reply']; print(m[0]['text'] if m else '')")
  [[ -n $REPLY ]] && break
  sleep 1
done
echo "   agent: ${REPLY:-(no reply within 30 seconds)}"
echo "   preferences: $(call GET /v1/users/$USER_ID/preferences '' "$TOKEN")"

echo "21. The same kind of request again. Now the user hears nothing while the worker waits: only what came of it."
BEFORE=$(notifications)
call POST /v1/users/$USER_ID/messages '{"text": "task: refund order 8888, needs approval"}' "$TOKEN" > /dev/null
for _ in $(seq 1 30); do
  ACTION=$(call GET "/v1/users/$USER_ID/actions" '' "$TOKEN" | python3 -c "import json,sys; a=json.load(sys.stdin)['actions']; print(a[0]['action_id'] if a else '')")
  [[ -n $ACTION ]] && break
  sleep 1
done
sleep 3
echo "   the worker waits for the user, and the main agent has sent $(( $(notifications) - BEFORE )) notifications about it"
echo "   the action is now: $(call POST "/v1/users/$USER_ID/actions/$ACTION/resolve" '{"decision": "approve"}' "$TOKEN" | json "['status']")"
echo "   notification: $(wait_for_notification "$BEFORE")"

echo "22. In their own client the user turns it up again."
call PUT /v1/users/$USER_ID/preferences '{"proactivity": "proactive"}' "$TOKEN"; echo

echo "23. With the API key, a task shows what was said about it, and by whom. The two latest tasks:"
for TASK in $(call GET "/v1/users/$USER_ID/tasks?status=all" | python3 -c "import json,sys; print(' '.join(t['task_id'] for t in json.load(sys.stdin)['tasks'][:2]))"); do
  call GET "/v1/users/$USER_ID/tasks/$TASK" \
    | python3 -c "import json,sys; t=json.load(sys.stdin); print('  ', t['title']); [print('    ', n['seq'], n['from'], n['kind'] + ':', n['text'][:160]) for n in t['notes']]"
done

echo "24. Everything that happened for the user, in order:"
call GET "/v1/users/$USER_ID/updates?after=0" '' "$TOKEN" \
  | python3 -c "import json,sys; [print('  ', x['seq'], x['type'], x['data'].get('kind', x['data'].get('status', ''))) for x in json.load(sys.stdin)['updates']]"
