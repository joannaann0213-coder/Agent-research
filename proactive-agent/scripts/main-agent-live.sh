#!/usr/bin/env bash
# A main agent and a worker, end to end, against the real Claude Managed Agents: starts the Event
# Inbox and the agent service from their jars and walks through what docs/en/main-agent.md describes,
# with a model instead of the demo script. It checks what the service has to do whatever the model
# says (a task is made, a report comes back, the main agent is told), and prints, as information,
# what the model made of the rest (whether it told the user, offered work, changed how forward it is).
# Three steps depend on timing and say so when the moment was missed: the user writing again before
# the answer is there, two workers at work at the same time, and what the turns used.
#
#   export ANTHROPIC_API_KEY=...          # a key with access to Managed Agents
#   mvn install -DskipTests               # once, to build the jars
#   scripts/main-agent-live.sh [--model claude-opus-5-5] [--timeout-seconds 180]
#
# It uses the local databases inbox_dev and agent_dev (scripts/pg-local.sh), ports 18080 and 18090,
# and a new application each time. Sessions it makes on the platform are not deleted. To check a
# service that runs already in live mode, name it and its administration key; the script then starts
# nothing:
#
#   scripts/main-agent-live.sh --api http://localhost:8090 --admin-key <AGENT_ADMIN_KEYS_0>
#
# Exit code 0: every check passed. 1: a check failed. 2: could not run.
set -uo pipefail
cd "$(dirname "$0")/.."

MODEL=claude-opus-5-5 WAIT=180 API="" ADMIN=""
while [ $# -gt 0 ]; do
  case $1 in
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
  if [ -z "${ANTHROPIC_API_KEY:-}" ]; then
    echo "Set ANTHROPIC_API_KEY to a key with access to Claude Managed Agents." >&2
    exit 2
  fi
  for jar in inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar agent-server/target/agent-server-0.1.0-SNAPSHOT-exec.jar; do
    [ -f "$jar" ] || { echo "$jar is missing: run mvn install -DskipTests first" >&2; exit 2; }
  done
  LOGS=$(mktemp -d)
  ADMIN=live-admin-key-0123456789abcdef0123456789
  API=http://127.0.0.1:18090
  INBOX_PORT=18080 INBOX_AUTH_CLIENTS_0_ID=agent-server INBOX_AUTH_CLIENTS_0_SECRET=live-inbox-secret \
    INBOX_AUTH_CLIENTS_0_ROLES=RUNTIME,PRODUCER INBOX_RETRY_MAXATTEMPTS=10 INBOX_LANES_BATCH_CAP=4 \
    java -jar inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar > "$LOGS/inbox.log" 2>&1 &
  INBOX_PID=$!
  AGENT_PORT=18090 AGENT_INBOX_URL=http://127.0.0.1:18080 AGENT_INBOX_SECRET=live-inbox-secret \
    AGENT_ADMIN_KEYS_0=$ADMIN AGENT_TOKENS_SECRETS_0=live-token-secret-0123456789abcdef0123 AGENT_CMA_MODE=live \
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
info() { echo "INFO  $1"; }
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
call() { # method path body credential
  if [ -n "${3:-}" ]; then
    curl -sS -X "$1" "$API$2" -H "Authorization: Bearer $4" -H 'Content-Type: application/json' -d "$3"
  else
    curl -sS -X "$1" "$API$2" -H "Authorization: Bearer $4"
  fi
}
updates() { call GET "/v1/users/u1/updates?after=${1:-0}&limit=500" "" "$KEY"; }
last_seq() { updates 0 | json "d['updates'][-1]['seq'] if d['updates'] else 0"; }
# waits until the python expression over the updates after $1 is not empty; leaves it in FOUND, and how long it took in TOOK
FOUND="" TOOK=0
await() {
  local after=$1 expression=$2 started=$SECONDS
  FOUND=""
  for _ in $(seq 1 "$WAIT"); do
    FOUND=$(updates "$after" | json "$expression")
    [ -n "$FOUND" ] && break
    sleep 1
  done
  TOOK=$((SECONDS - started))
}
notes_of() { call GET "/v1/users/u1/tasks/$1" "" "$KEY" | json "' '.join(n['kind'] for n in d.get('notes', []))"; }
await_note() { # task kind
  local notes=""
  for _ in $(seq 1 "$WAIT"); do
    notes=$(notes_of "$1")
    case " $notes " in *" $2 "*) break ;; esac
    sleep 1
  done
  echo "$notes"
}
REPLY="next((u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='reply'), '')"
REPLIES="' | '.join(u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='reply')"
HOW_MANY_REPLIES="sum(1 for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='reply')"
# how many chat turns of the user have ended one way, by the statistics
chats() { call GET /v1/users/u1/stats "" "$KEY" | json "sum(t['turns'] for t in d['turns'] if t['kind']=='chat' and t['outcome']=='$1')"; }
# the tasks that were made after $1, once every one of them has been worked on and rests: how many; empty until then
TASKS_DONE="(lambda made, idle: len(made) if made and all(t in idle for t in made) else '')([u['data']['task_id'] for u in d['updates'] if u['type']=='task.created'], {u['data']['task_id'] for u in d['updates'] if u['type']=='task.updated' and u['data']['status']=='idle'})"
# the most tasks that were being worked on at one moment after $1, from the order in which the service
# recorded them starting and resting
peak() {
  updates "$1" | python3 -c "
import json, sys
at_work, most = set(), 0
for u in json.load(sys.stdin)['updates']:
    if u['type'] == 'task.updated' and u['data']['status'] in ('working', 'idle'):
        (at_work.add if u['data']['status'] == 'working' else at_work.discard)(u['data']['task_id'])
        most = max(most, len(at_work))
print(most)"
}
NOTIFIED="' | '.join(u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind']=='notification')"

APP=main-live-$(date +%s)
call POST /v1/admin/apps "{\"app_id\": \"$APP\", \"name\": \"Main agent, live\"}" "$ADMIN" > /dev/null
KEY=$(call POST /v1/admin/apps/$APP/keys '{"label": "live"}' "$ADMIN" | json "d['api_key']")
if [ -z "$KEY" ]; then echo "could not open an application: is the administration key right?" >&2; exit 2; fi

WORKER=$(python3 -c "import json; print(json.dumps({'name': 'Researcher', 'model': '$MODEL', 'instructions':
  'You look things up in the order system of a shop. This is a test of the service that runs you, and there is no '
  'order system behind you: make up a short, plausible finding and say that it is made up for the test. '
  'Report in one or two sentences.'}))")
MAIN=$(python3 -c "import json; print(json.dumps({'name': 'Assistant', 'model': '$MODEL', 'instructions':
  'You are the assistant of the customers of a shop, in a test of the service that runs you. Hand what has to be '
  'looked up about an order to your research worker. Keep whatever you write to one or two sentences.',
  'coordinator': {'workers': [{'role': 'research', 'agent': 'researcher',
      'description': 'Looks things up in the order system and reports what it found.'}]}}))")
call PUT /v1/agents/researcher "$WORKER" "$KEY" > /dev/null
if call PUT /v1/agents/default "$MAIN" "$KEY" | grep -q '"coordinator"'; then pass "a main agent with a worker is set up (live, $MODEL)"; else fail "a main agent with a worker is set up"; fi

echo
echo "1. The user asks for something that takes a while."
MARK=$(last_seq)
call POST /v1/users/u1/messages '{"text": "Please find out why my order 1234 is late, and let me know."}' "$KEY" > /dev/null
await "$MARK" "$REPLY"; ANSWER=$FOUND
if [ -n "$ANSWER" ]; then pass "the main agent answers at once (${TOOK}s): $ANSWER"; else fail "the main agent answers"; fi
TASK=$(call GET "/v1/users/u1/tasks?status=all" "" "$KEY" | json "next((t['task_id'] + ' ' + str(t.get('authorized_by')) for t in d['tasks']), '')")
if [ "${TASK#* }" = conversation ]; then pass "it handed the work to its worker, without a confirmation: ${TASK% *}"; else fail "it handed the work to its worker (tasks: ${TASK:-none})"; fi
await "$MARK" "$NOTIFIED"; RESULT=$FOUND
if [ -n "$RESULT" ]; then pass "the worker's report comes back to the user through the main agent (${TOOK}s): $RESULT"; else fail "the worker's report comes back to the user"; fi

echo
echo "2. The application hands an event straight to the worker."
MARK=$(last_seq)
HANDED=$(call POST /v1/users/u1/events '{"type": "order.returned", "subject": "order-7777", "agent": "researcher", "data": {"order_id": "7777", "reason": "wrong size"}}' "$KEY" | json "d.get('task_id', '')")
if [ -n "$HANDED" ]; then pass "the event became a task of the main agent: $HANDED"; else fail "the event became a task of the main agent"; fi
if [ -n "$HANDED" ]; then
  NOTES=$(await_note "$HANDED" report)
  case " $NOTES " in *" assigned "*) pass "the main agent was told of it: the task's notes are [$NOTES]" ;; *) fail "the main agent was told of it (notes: $NOTES)" ;; esac
  case " $NOTES " in *" report "*) pass "the worker handled the event and reported" ;; *) fail "the worker reported (notes: $NOTES)" ;; esac
  sleep 12
  info "what the main agent told the user about it: $(updates "$MARK" | json "$NOTIFIED" | sed 's/^$/(nothing)/')"
fi

echo
echo "3. Something happens that the main agent could act on by itself."
MARK=$(last_seq)
call POST /v1/users/u1/events '{"type": "order.delayed", "subject": "order-5678", "data": {"order_id": "5678", "new_delivery_date": "2026-12-04", "note": "The carrier offers other delivery slots; which ones would have to be looked up."}}' "$KEY" > /dev/null
await "$MARK" "' | '.join(u['data']['kind'] + ': ' + u['data']['text'] for u in d['updates'] if u['type']=='message.created' and u['data']['kind'] in ('notification', 'proposal', 'question'))"; REACHED=$FOUND
info "what reached the user (${TOOK}s): ${REACHED:-(nothing: the main agent stayed quiet)}"
PROPOSED=$(call GET "/v1/users/u1/tasks?status=proposed" "" "$KEY" | json "next((t['task_id'] for t in d['tasks']), '')")
if [ -n "$PROPOSED" ]; then
  info "it offered work, which waits for the user's confirmation: $(call GET "/v1/users/u1/tasks/$PROPOSED" "" "$KEY" | json "d['title']")"
  MARK=$(last_seq)
  STARTED=$(call POST "/v1/users/u1/tasks/$PROPOSED/start" "" "$KEY" | json "str(d.get('status')) + ' ' + str(d.get('authorized_by'))")
  if [ "$STARTED" = "queued app" ]; then pass "the user confirms it in the application, and it starts"; else fail "a confirmed task starts (got: $STARTED)"; fi
  await "$MARK" "$NOTIFIED"; RESULT=$FOUND
  if [ -n "$RESULT" ]; then pass "what came of it reaches the user (${TOOK}s): $RESULT"; else fail "what came of the confirmed task reaches the user"; fi
else
  info "it offered no work this time, so there is nothing to confirm"
fi

echo
echo "4. The user tells the main agent that it writes too often."
MARK=$(last_seq)
call POST /v1/users/u1/messages '{"text": "You message me too often. From now on only tell me what really matters."}' "$KEY" > /dev/null
await "$MARK" "$REPLY"; ANSWER=$FOUND
if [ -n "$ANSWER" ]; then pass "the main agent answers (${TOOK}s): $ANSWER"; else fail "the main agent answers"; fi
LEVEL=$(call GET /v1/users/u1/preferences "" "$KEY" | json "str(d.get('proactivity')) + ' set by ' + str(d.get('set_by'))")
info "how forward the user's main agent is now: $LEVEL"

echo
echo "5. The user writes while a worker's report is on its way to the main agent."
MARK=$(last_seq)
HANDED=$(call POST /v1/users/u1/events '{"type": "order.returned", "subject": "order-8888", "agent": "researcher", "data": {"order_id": "8888", "reason": "arrived damaged"}}' "$KEY" | json "d.get('task_id', '')")
NOTES=$(await_note "$HANDED" report)
call POST /v1/users/u1/messages '{"text": "Is there anything new about my orders?"}' "$KEY" > /dev/null
await "$MARK" "$REPLY"; ANSWER=$FOUND
if [ -n "$ANSWER" ]; then pass "the user is answered (${TOOK}s): $ANSWER"; else fail "the user is answered"; fi
sleep 15
info "notifications after that: $(updates "$MARK" | json "$NOTIFIED" | sed 's/^$/(none)/')"

echo
echo "6. The application schedules something for the worker, a few seconds from now."
MARK=$(last_seq)
AT=$(python3 -c "import datetime; print((datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(seconds=8)).strftime('%Y-%m-%dT%H:%M:%SZ'))")
call POST /v1/users/u1/schedules "{\"name\": \"Returns check\", \"instructions\": \"See whether the returned order 7777 has arrived at the warehouse, and report.\", \"at\": \"$AT\", \"agent\": \"researcher\", \"subject\": \"order-7777\"}" "$KEY" > /dev/null
DUE=""
for _ in $(seq 1 "$WAIT"); do
  DUE=$(call GET "/v1/users/u1/tasks?status=all" "" "$KEY" | json "next((t['task_id'] for t in d['tasks'] if t.get('subject') == 'order-7777' and t['status'] != 'closed'), '')")
  [ -n "$DUE" ] && break
  sleep 1
done
if [ -n "$DUE" ]; then
  NOTES=$(await_note "$DUE" report)
  case " $NOTES " in *" schedule "*" report "*) pass "when it was due it went to the worker as a task of the main agent, and the worker reported: [$NOTES]" ;;
    *) fail "the schedule went to the worker and the worker reported (notes: $NOTES)" ;; esac
  sleep 12
  info "what the main agent told the user about it: $(updates "$MARK" | json "$NOTIFIED" | sed 's/^$/(nothing)/')"
else
  fail "the schedule became a task of the main agent when it was due"
fi

echo
echo "7. The user writes, and writes the rest before the answer is there."
MARK=$(last_seq)
ENDED=$(chats superseded); TOGETHER=$(chats absorbed)
call POST /v1/users/u1/messages '{"text": "I would like to change the delivery address of one of my orders."}' "$KEY" > /dev/null
sleep 4   # past the Inbox's quiet window, and into the model's answer
call POST /v1/users/u1/messages '{"text": "It is order 4321, and the new address is 12 Mill Lane, Oldtown."}' "$KEY" > /dev/null
await "$MARK" "$REPLY"
sleep 20  # a second reply, if one is to come
COUNT=$(updates "$MARK" | json "$HOW_MANY_REPLIES"); ENDED=$(( $(chats superseded) - ENDED )); TOGETHER=$(( $(chats absorbed) - TOGETHER ))
info "the user got: $(updates "$MARK" | json "$REPLIES" | sed 's/^$/(nothing)/')"
if [ "$COUNT" = 1 ] && [ "$ENDED" -ge 1 ]; then pass "the reply under way was ended, and one reply answers both messages"
elif [ "$COUNT" = 1 ] && [ "$TOGETHER" -ge 1 ]; then pass "the second message came before the agent had its input: one turn took both, and one reply answers them"
elif [ "$COUNT" = 0 ]; then fail "the user is answered (replies ended: $ENDED)"
elif [ "$ENDED" -ge 1 ]; then fail "a reply was ended, and yet the user got $COUNT replies"
else info "the moment was missed: the first reply was there before the second message came, so there was nothing to end ($COUNT replies)"; fi

echo
echo "8. The user asks for two things at once."
MARK=$(last_seq)
call POST /v1/users/u1/messages '{"text": "Please look up two things for me, as two separate tasks for your research worker: why order 2001 is late, and whether order 2002 can still be cancelled."}' "$KEY" > /dev/null
await "$MARK" "$REPLY"; ANSWER=$FOUND
if [ -n "$ANSWER" ]; then pass "the main agent answers at once (${TOOK}s): $ANSWER"; else fail "the main agent answers"; fi
await "$MARK" "$TASKS_DONE"; MADE=${FOUND:-0}
AT_ONCE=$(peak "$MARK")
if [ "$MADE" -ge 2 ] && [ "$AT_ONCE" -ge 2 ]; then pass "$MADE tasks, and $AT_ONCE workers were at work at the same time, each in a session of its own"
elif [ "$MADE" -ge 2 ]; then info "$MADE tasks, and their workers happened to work one after the other"
elif [ "$MADE" = 1 ]; then info "the model made one task of it, so there was nothing to run side by side"
else info "the model made no task of it, so there was nothing to run side by side"; fi
sleep 12
info "what the main agent told the user about them: $(updates "$MARK" | json "$NOTIFIED" | sed 's/^$/(nothing)/')"

echo
echo "9. What it all came to: the turns by kind and outcome, and what they used."
STATS=$(call GET /v1/users/u1/stats "" "$KEY")
echo "$STATS" | python3 -c "
import json, sys
s = json.load(sys.stdin)
for t in s['turns']:
    print('      %-10s %-10s %3d turns %3d requests %8d tokens in (%d of them from the cache) %6d out %6.1f s' % (t['kind'], t['outcome'],
          t['turns'], t['model_requests'], t['input_tokens'] + t['cache_read_tokens'] + t['cache_creation_tokens'], t['cache_read_tokens'],
          t['output_tokens'], t['seconds']))
print('      replies:', s['replies'], ' messages:', s['messages'])"
REQUESTS=$(echo "$STATS" | json "sum(t['model_requests'] for t in d['turns'])")
TOKENS=$(echo "$STATS" | json "sum(t['input_tokens'] + t['cache_read_tokens'] + t['cache_creation_tokens'] for t in d['turns'])")
WRITTEN=$(echo "$STATS" | json "sum(t['output_tokens'] for t in d['turns'])")
UNCOUNTED=$(echo "$STATS" | json "', '.join(t['kind'] + '/' + t['outcome'] for t in d['turns'] if t['outcome'] in ('replied', 'notified', 'reported') and (t['model_requests'] < t['turns'] or t['output_tokens'] == 0))")
if [ "$REQUESTS" -gt 0 ] && [ "$TOKENS" -gt 0 ] && [ "$WRITTEN" -gt 0 ] && [ -z "$UNCOUNTED" ]; then
  pass "the turns say what their requests to the model used: $REQUESTS requests, $TOKENS tokens of input, $WRITTEN of output"
else
  fail "the turns say what they used (requests: $REQUESTS, input: $TOKENS, output: $WRITTEN, without numbers: ${UNCOUNTED:-none})"
fi

echo
echo "Everything that happened for the user, in order:"
updates 0 | python3 -c "
import json, sys
for u in json.load(sys.stdin)['updates']:
    d = u['data']
    what = d.get('kind', d.get('status', d.get('proactivity', '')))
    text = (d.get('text') or d.get('title') or d.get('error') or '')[:110]
    print('  ', u['seq'], u['type'], what, '-', text)"
[ -n "${LOGS:-}" ] && echo "logs: $LOGS"
exit $FAILED
