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
| Conversation | What a user and the agent say to each other, by `conversation_id`; `default` unless named |
| Event | Something that happened in the application's systems that concerns a user. Events of one `subject` are handled by an agent that remembers the earlier ones |
| Action | What the agent waits for a person to decide: an approval of a tool call, or a question to the user |
| Schedule | What the agent is to do at a given time: set up by the application, or a follow-up the agent set itself |
| Update | What happened for a user that the application may want to know: a message, an action, a failure. Read by polling, by a stream, or at a webhook |
| Credential | What an MCP server needs to let the agent in. Kept in vaults on the agent platform, never by this service |

## Authentication

Every `/v1` request carries a credential as `Authorization: Bearer <credential>`.

| Credential | Looks like | Who has it | What it may do |
| --- | --- | --- | --- |
| Administration key | 32 characters or more, from the configuration | The operators of the service | `/v1/admin` and nothing else: applications and their API keys |
| API key | `afk.<key_id>.<secret>` | The application's back end | Everything of the application, for every user |
| User token | `aft.<payload>.<signature>` | One user's client, which gets it from the application's back end | That user's messages, actions, updates, schedules and credentials; not events, not the agents |

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

## Updates

Everything that happens for a user is recorded as an update: `message.created`, `action.created`,
`action.resolved`, `run.failed`. Each has a `seq` that only grows, so a reader that resumes after the
last one it got misses nothing. There are three ways to read them.

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

The service gives every agent four tools of its own: `read_events`, `notify_user`, `ask_user` and
`schedule_followup`. A conversation that is under way takes a changed configuration on at its next
turn, in a new session that is told what was said before.

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
| Message text, event data | 32,000 characters, 16 KB | fixed |
