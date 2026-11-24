# Agent API

[中文](../zh/agent-api.md)

The Agent API opens the proactive agent to applications. An application's back end, or its users'
own clients, send what users say and what happens in the application's systems. The agent answers,
speaks first when there is a reason to, asks before it does what needs somebody's approval, and comes
back later when it said it would. It runs on Claude Managed Agents, which the application never deals
with directly.

This is the guide for application developers. Every endpoint, field and status is specified in the
OpenAPI document; [Agent service](agent-service.md) explains how the service works inside.
[The tutorial](tutorial.md) runs the service and connects an application step by step.

## OpenAPI specification

The service publishes its specification at `GET /openapi.yaml`, without authentication. It is the
contract: the test suite checks every request and response of its integration tests against it, and
fails when an operation, or a status the specification lists, is not exercised.

```bash
curl -s http://localhost:8090/openapi.yaml -o agent-api.yaml
```

## Concepts

| Concept | What it is |
| --- | --- |
| Application | One of your products, with its own agents, users and keys. An operator of the service creates it |
| User | One of the application's end users, under the application's own id (`user_id`). A user exists from the first time the application mentions them |
| Agent | What the agent is for and what it may use: instructions, model, MCP servers, a sandbox. An application can have several, by key; `default` unless named |
| Main agent | An agent that has workers. Everything that concerns a user comes to it, in the conversation it serves, and it hands longer work to its workers |
| Conversation | What a user and the agent say to each other, by `conversation_id`; `default` unless named |
| Event | Something that happened in the application's systems that concerns a user. Events of one `subject` are handled by an agent that remembers the earlier ones |
| Action | What the agent waits for a person to decide: an approval of a tool call, or a question to the user |
| Schedule | What the agent is to do at a given time: set up by the application, or a follow-up the agent set itself |
| Task | A piece of work that a worker does under a main agent: another agent of the application, in a session of its own. The main agent hands it on, or the application does, with an event or a schedule for that agent; either way the main agent knows of it. What the main agent begins on its own initiative waits for the user's confirmation first |
| Grant | A user's standing authorisation: their main agent may start the tasks of one role on its own initiative, without asking each time |
| Update | What happened for a user that the application may want to know: a message, an action, a failure. Read by polling, by a stream, or at a webhook |
| Credential | What an MCP server needs to let the agent in. Kept in vaults on the agent platform, never by this service |

## Authentication

Every `/v1` request carries a credential as `Authorization: Bearer <credential>`.

| Credential | Looks like | Who has it | What it may do |
| --- | --- | --- | --- |
| Administration key | 32 characters or more, from the configuration | The operators of the service | `/v1/admin` and nothing else: applications and their API keys |
| API key | `afk.<key_id>.<secret>` | The application's back end | Everything of the application, for every user |
| User token | `aft.<payload>.<signature>` | One user's client, which gets it from the application's back end | That user's messages, actions, updates, schedules, tasks, grants and credentials; not events, not the agents |

An API key is shown once, when it is made, and stored as a hash. A user token lasts 15 minutes by
default and at most an hour; the back end gets one with `POST /v1/users/{user_id}/tokens` and revokes
all of a user's tokens with `DELETE` on the same path. A web page on another origin than the service
can use one once its origin is in `AGENT_CORS_ALLOWEDORIGINS`.

## Errors

An error is `application/problem+json` with a stable `code`.

| Status | `code` | When |
| --- | --- | --- |
| 400 | `invalid_request` | The request is not valid; `detail` says why |
| 401 | `unauthenticated` | No credential, or one that is unknown, expired or revoked |
| 403 | `forbidden` | The credential may not do this, or the application is suspended |
| 404 | `not_found` | No such thing, or none that the caller may see |
| 409 | `conflict` | An id was used for something else, or a decision was taken already |
| 413 | `payload_too_large` | A text or data is too long |
| 429 | `rate_limited` | Too many requests; wait as many seconds as `Retry-After` says |
| 503 | `unavailable` | The agent platform cannot be reached; nothing was changed, try again |

## Quick start

The service and the Event Inbox run as in [the README](../../README.md#agent-service); in demo mode
no Anthropic key is needed and a script plays the agent. `examples/agent-api/quickstart.sh` runs the
same steps.

```bash
API=http://localhost:8090
ADMIN=change-me-admin-key-0123456789abcdef

# An operator registers the application and gives its back end an API key.
curl -s -X POST $API/v1/admin/apps -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{"app_id": "acme", "name": "Acme"}'
KEY=$(curl -s -X POST $API/v1/admin/apps/acme/keys -H "Authorization: Bearer $ADMIN" \
  -H 'Content-Type: application/json' -d '{"label": "back end"}' | jq -r .api_key)

# The application describes its agent.
curl -s -X PUT $API/v1/agents/default -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"name": "Assistant", "instructions": "You help Acme customers with their orders."}'

# A user writes; 202 means stored, not answered.
curl -s -X POST $API/v1/users/u42/messages -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' -d '{"message_id": "m-1", "text": "Where is my order 1234?"}'

# A moment later the reply is among the messages.
curl -s "$API/v1/users/u42/messages?after=0" -H "Authorization: Bearer $KEY"
```

## Conversations

`POST /v1/users/{user_id}/messages` stores a message and gets it to the agent; the answer appears
among the user's messages with `kind: reply`. Read them with `GET /v1/users/{user_id}/messages`,
passing `after` set to the last `next_after`, or follow the [updates](#updates).

- `message_id` makes a retry harmless: the same id and text return the earlier receipt with
  `deduped: true`; the same id with another text is a 409.
- Messages that arrive close together (within about a second and a half) are answered together, in
  one reply. A message that arrives while the agent answers is not pushed into that answer; it gets
  the next one.
- Each `conversation_id` is a conversation of its own, with a session of its own on the platform.
  The agent of a conversation is fixed by its first message (`agent`).
- `POST /v1/users/{user_id}/conversations/{conversation_id}/stop` stops the reply under way: the
  user's messages so far are not answered any more. A message sent afterwards is answered as usual.

A user's client can do all of this itself with a user token, for its own user only.

## Events from your systems

`POST /v1/users/{user_id}/events` tells the agent that something happened that concerns the user:
an order shipped, a ticket was answered, a payment failed. Only an API key may do it.

```json
{
  "event_id": "order-1234-delayed",
  "type": "order.delayed",
  "subject": "order-1234",
  "data": {"order_id": "1234", "new_delivery_date": "2026-11-27", "reason": "carrier backlog"}
}
```

The agent looks at it in the background, on the line of its `subject`, and decides whether the user
needs to hear about it. If so, it sends a message with `kind: notification` to the conversation that
`conversation_id` names (`default` unless named). Often it decides that nothing needs saying.

- `data` reaches the agent as information from another system, never as instructions: text in it
  that tells the agent what to do is not obeyed. Put what the agent should do with a kind of event in
  the agent's `instructions`.
- `deliver_at` holds an event back until its time.
- The same `event_id` with the same content is harmless; with other content it is a 409. Types that
  start with `agent.` are the service's own.
- A conversation learns what work in the background exchanged with the user in it, before it answers
  the user's next message.
- An event for a [main agent](#a-main-agent-and-its-tasks) is not handled on a line of its `subject`:
  it goes to the conversation that `conversation_id` names, where the main agent hears of everything.
- In a conversation that a main agent serves, an event that names another agent (`agent`) has no
  line of its own either. It becomes a task of the main agent: the named agent handles it as a
  worker, the main agent is told at once, and the receipt has the task's `task_id`.

## Actions

When the agent wants to call a tool whose server is configured with `approval: always_ask` (or
`auto`, when the platform cannot judge the call alone), or asks the user something with its
`ask_user` tool, the turn waits for a person, and an action appears.

| Kind | Who decides | How |
| --- | --- | --- |
| `approval` | The `approver` of the MCP server in the agent's configuration: `user` or `app` | `{"decision": "approve"}` or `{"decision": "deny", "message": "why"}` |
| `question` | The user | `{"answer": "..."}`, or a message to the conversation, which answers the latest question; `{"decision": "deny"}` declines it |

`GET /v1/users/{user_id}/actions` lists what waits; `POST .../actions/{action_id}/resolve` decides.
An API key may decide every action of its users; a user token only what is the user's to decide, and
it does not see the rest.

- Whoever decides first decides, and a decision is never undone: an approval does not turn into a
  denial because a deadline or new input came at the same moment.
- An action that nobody decides within 24 hours expires, and the agent is told so.
- New input on the action's line (a message in the conversation, a new event for the subject)
  withdraws what waits; the agent finishes its turn, and the new input comes in the next one.
- A question of a [main agent](#a-main-agent-and-its-tasks) is not withdrawn by events or by its
  workers' reports: it waits for the user's next message in the conversation, or for an answer with
  `resolve`, and the agent replies to that as to any message.
- Built-in sandbox tools that would need an approval are not put before anybody: they are denied.

## Schedules and follow-ups

`POST /v1/users/{user_id}/schedules` has the agent do something at a given time, with your
instructions: once (`at`), or again and again on a `cron` rule read in the user's `time_zone`.

```json
{
  "schedule_id": "morning-brief",
  "name": "Morning brief",
  "instructions": "Look at the user's open orders and tell them about anything that needs them today.",
  "cron": "0 8 * * 1-5",
  "time_zone": "Asia/Shanghai",
  "subject": "orders"
}
```

The agent also sets follow-ups for itself with its `schedule_followup` tool: "remind me in an hour"
in a conversation, or "look at this order again tomorrow" in the background. Both kinds are listed
by `GET /v1/users/{user_id}/schedules` and cancelled with `DELETE .../schedules/{schedule_id}`; a
user's client may do both, so that users can stop what the agent does for them.

In a conversation that a [main agent](#a-main-agent-and-its-tasks) serves, a schedule that names
another agent (`agent`) is that agent's to do, as a worker. Each time it is due it is handed to the
worker as part of a task of the main agent, kept under the schedule's `subject`; the main agent is
told at once, and tells the user what the worker reports if they need to hear it.

## Updates

Everything that happens for a user is recorded as an update: `message.created`, `action.created`,
`action.resolved`, `run.failed`, and for the tasks of a main agent `task.created` and `task.updated`.
Each has a `seq` that only grows, so a reader that resumes after the last one it got misses nothing.
There are three ways to read them.

| Way | Endpoint | For |
| --- | --- | --- |
| Polling | `GET /v1/users/{user_id}/updates?after=<seq>` | Anything; pages of up to 500 |
| Stream | `GET /v1/users/{user_id}/stream` | Clients that show replies as they come; server-sent events |
| Webhook | `PUT /v1/webhook` | The application's back end; every user of the application |

The stream sends each update as an event whose `id` is its `seq`. A client that connects again with
`Last-Event-ID` gets what it missed; a browser's `EventSource` does that by itself. `EventSource`
cannot send a header, so a browser first gets a one-time ticket with
`POST /v1/users/{user_id}/stream-tickets` and opens `GET /v1/users/{user_id}/stream?ticket=<ticket>`.

The webhook gets a POST per update, signed with HMAC-SHA256: `webhook-signature` is `v1,` followed by
the Base64 signature of `<webhook-id>.<webhook-timestamp>.<body>`, keyed with the secret's bytes
(the part after `whsec_`, Base64-decoded). Java receivers can use `WebhookSignature.verify` from
`agent-api`; in Python:

```python
import base64, hashlib, hmac, time

def verify(secret: str, headers: dict, body: bytes, tolerance: int = 300) -> bool:
    key = base64.b64decode(secret.removeprefix("whsec_"))
    signed = f"{headers['webhook-id']}.{headers['webhook-timestamp']}.".encode() + body
    expected = "v1," + base64.b64encode(hmac.new(key, signed, hashlib.sha256).digest()).decode()
    fresh = abs(time.time() - int(headers["webhook-timestamp"])) <= tolerance
    return fresh and any(hmac.compare_digest(expected, s) for s in headers["webhook-signature"].split(" "))
```

- Answer with any 2xx. Anything else is tried again after 10 s, 1 min, 5 min, 30 min, 2 h, 6 h and
  12 h; when the last attempt fails, the webhook is turned off until a PUT with `enabled: true`.
- A delivery can come twice: ignore an `id` you have seen. Order by `seq`, per user.
- The URL must be https on the public internet. Loopback, private and link-local addresses (cloud
  metadata services included) are refused, when the webhook is set up and again for every delivery;
  redirects are not followed.

## Credentials for MCP servers

An MCP server that needs a credential gets it from a vault on the agent platform, which adds it to
the agent's calls of the server; neither the agent nor its sandbox ever sees it, and this service
does not keep it.

```bash
# The application's credential: every user's sessions have it.
curl -s -X PUT $API/v1/credentials/tickets -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"mcp_server_url": "https://mcp.acme.example/tickets", "token": "tk_live_example"}'

# A user's own account, after the user connected it with OAuth in your application.
curl -s -X PUT $API/v1/users/u42/credentials/notes -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"mcp_server_url": "https://mcp.notes.example/mcp",
       "oauth": {"access_token": "at_example", "refresh_token": "rt_example",
                 "client_id": "acme-agent", "token_endpoint": "https://auth.notes.example/oauth/token"}}'
```

A user's credential comes before the application's for the same server. A replaced token is used
from the next call on. With a refresh token, the platform renews the access token by itself.

## Agents

`PUT /v1/agents/{agent_key}` describes an agent; the whole configuration each time.

| Field | Meaning |
| --- | --- |
| `name`, `instructions` | Required. What the agent is for and how it behaves. The service adds short notes of its own on how the agent is woken and how it reaches the user |
| `model`, `effort` | The Claude model (`claude-opus-5-5` by default) and how hard it thinks |
| `mcp_servers` | Third-party systems: `name`, an https `url`, `approval` (`always_ask`, `auto` or `always_allow`) and `approver` (`app` or `user`) |
| `sandbox` | A container with built-in tools (shell, files, web search and fetch) and its network: `none`, `limited` to `allowed_hosts`, or `unrestricted` |
| `session_budget_usd` | What one session may spend at most; a conversation whose session has spent it goes on in a new one |
| `memory` | `per_user`: the agent keeps notes about each user across sessions, in files of its sandbox, which must be enabled. It writes them in conversations and only reads them in the background |
| `coordinator` | Makes the agent a main agent. `workers` lists who it may hand work to: each with a `role`, the key of the `agent` that plays it, a `description` that tells the main agent what the role is for, and if wanted `start`, the rule for when a task of the role may start |

The service gives every agent four tools of its own: `read_events`, `notify_user`, `ask_user` and
`schedule_followup`; a main agent gets five more to hand work on, `delegate_task`, `start_task`,
`message_worker`, `list_tasks` and `close_task`. A conversation that is under way takes a changed
configuration on at its next turn, in a new session that is told what was said before.

## A main agent and its tasks

An agent with a `coordinator` is a main agent: the one agent that hears of everything about a user
and decides what to do about it. It answers the user, your events and schedules wake it in the same
session, and it hands work that takes long to workers, which report back to it.
[Main agent and workers](main-agent.md) explains how that works and why it is built this way.

```bash
# A worker is an ordinary agent, with instructions and tools of its own.
curl -s -X PUT $API/v1/agents/researcher -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"name": "Researcher", "instructions": "You look things up in the order system and report what you found."}'

# The main agent names the workers it may hand work to, and what each is for.
curl -s -X PUT $API/v1/agents/default -H "Authorization: Bearer $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"name": "Assistant", "instructions": "You look after Acme customers.",
       "coordinator": {"workers": [{"role": "research", "agent": "researcher",
                                    "description": "Looks things up in the order system and reports what it found."}]}}'

# What the main agent has handed on for a user, and how to stop one of those tasks.
curl -s $API/v1/users/u42/tasks -H "Authorization: Bearer $KEY"
curl -s -X POST $API/v1/users/u42/tasks/task_01EXAMPLE/close -H "Authorization: Bearer $KEY"

# What waits for the user's confirmation, and confirming one of those tasks. To decline it, close it.
curl -s "$API/v1/users/u42/tasks?status=proposed" -H "Authorization: Bearer $KEY"
curl -s -X POST $API/v1/users/u42/tasks/task_01EXAMPLE/start -H "Authorization: Bearer $KEY"

# The user's standing authorisation for a role, and taking it back.
curl -s -X PUT $API/v1/users/u42/grants/default/research -H "Authorization: Bearer $KEY"
curl -s -X DELETE $API/v1/users/u42/grants/default/research -H "Authorization: Bearer $KEY"
```

Nothing else changes in how you call the API: users write to `POST /v1/users/{user_id}/messages` and
your systems post events, as before. What changes is where those go.

- Events and schedules for a main agent are handled in the conversation that `conversation_id` names
  (`default` unless named), by the session that talks to the user there. If another agent serves
  that conversation, the request is a 409.
- The main agent makes a task, and so does work of yours that names another agent (`agent`): an
  event, or a schedule each time it is due. That agent does it as a worker, in a task with
  `created_by: app` and the `subject` of the event or schedule, which starts without a confirmation.
  The main agent is told at once, and the worker reports to it. Later work of the subject goes to
  the same task while it is open.
- `GET /v1/users/{user_id}/tasks` lists the open tasks, with `status=all` the closed ones too, and
  `POST .../tasks/{task_id}/close` stops one; the main agent is told.
- What the user asks the main agent for starts at once. What the main agent begins on its own
  initiative is a task with the status `proposed`: show it to the user with its `summary`, and call
  `POST .../tasks/{task_id}/start` when they confirm it, or close it when they decline. It can be
  confirmed for 3 days. The summary is also a message of the conversation, with `kind: proposal` and
  the task's `task_id`, so a client that shows only the conversation shows it as well; there the
  user confirms by telling the agent to go ahead.
- With a grant the user lets the main agent start the tasks of a role without asking:
  `PUT /v1/users/{user_id}/grants/{agent_key}/{role}` gives it, and `DELETE` on the same path takes it
  back. A role's `start` sets the rule: `ask`, which is the default and what was just described,
  `always_ask`, which has every task of the role confirmed, or `always_allow`, which has none.
- A user's own client may list and close tasks. What a worker reported, and what the two agents
  wrote to each other, is returned to an API key only.
- The user hears of a task from the main agent. A worker cannot notify or ask the user, but its tool
  calls that need an approval appear among the [actions](#actions) as usual.
- A worker is any agent of the application that is not a main agent. Of the user and the
  conversation it knows only what the main agent writes into the task, or what your event says.
- The workers of one user run one at a time, unless the Inbox allows more (`inbox.lanes.batch.cap`).

In demo mode the script hands on whatever the user writes after `task:`, and the worker answers that
it is done; an event whose data has a text `task` makes the script propose that work by itself, and
an event or a schedule that names the worker is handled by the worker, whose report the main agent
passes on. `examples/agent-api/main-agent.sh` walks through all of these.

## Limits

| What | Default | Setting |
| --- | --- | --- |
| Requests of an application's API keys | 100 at once, then 50 a second | `agent.limits.app-burst`, `app-per-second` |
| Requests of one user's tokens | 20 at once, then 5 a second | `agent.limits.user-burst`, `user-per-second` |
| Notifications of one turn in the background | 3 | `agent.notify.per-run` |
| Notifications of one user | 20 in 24 hours | `agent.notify.per-user-per-day` |
| Follow-ups in a row, and waiting on one line | 5 and 5 | `agent.followups.max-chain`, `max-pending` |
| How long an action waits | 24 hours | `agent.actions.ttl` |
| How long a turn may work | 10 minutes | `agent.turn.deadline` |
| Tasks of one main agent's conversation: queued or worked on, and open | 4 and 20 | `max_active_tasks` of the agent, `agent.tasks.max-open` |
| Instructions of a task, and a worker's report | 8,000 and 16,000 characters | `agent.tasks.max-instructions`, `max-report` |
| Tasks of one main agent's conversation that wait for a confirmation, and for how long | 5, for 3 days | `agent.tasks.max-proposed`, `proposal-ttl` |
| Tasks that start for one user without the user being asked | 10 in 24 hours | `agent.tasks.max-unasked-per-day` |
| Message text, event data | 32,000 characters, 16 KB | fixed |
