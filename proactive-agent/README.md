# Proactive Agent: Event Inbox and agent service

[中文](README.zh-CN.md)

The Event Inbox is the inbound gate of a proactive agent. Every trigger (a webhook, a timer, a user
message, an approval reply) is appended to a per-user inbox through one write API, and the agent
runtime gets exactly one way to take work out of it: claim a batch of ready events as a run.

This repository holds roadmap stages **M0 and M1** of the inbox as a Spring Boot service on PostgreSQL,
and on top of it the **agent service**, which opens the proactive agent to applications through the
[Agent API](docs/en/agent-api.md) and runs it on Claude Managed Agents. An agent can be a user's
[main agent](docs/en/main-agent.md): everything about the user comes to it, and it hands longer work
to worker agents. The third tier of the multi-agent layer and long-term memory are described in
[the architecture](docs/en/architecture.md) but not built here. For the Inbox on its own, a scripted
worker stands in for the agent, so that its contract can be exercised end to end.

## What it guarantees

- **One write entry, append only.** Writes authenticate, validate, dedupe and append in one SQL
  statement. They never start a run and never call a model, so `202 Accepted` does not depend on
  anything downstream.
- **Idempotent.** One event per `(inbox, source_event_id)`. A repeat returns the earlier event; the same
  key with a different payload is a `409`.
- **Single flight.** At most one active run per merge key in an inbox, a cap per lane, and an optional
  inbox-wide cap. Sequence order equals commit order.
- **Batching.** Events with the same merge key that arrive inside a quiet window become one run.
- **At-least-once, replayable.** A run holds a lease. When the lease expires the same run, with the
  same id and the same events, is issued again, so a downstream outbox can dedupe on `run_id + sequence`.
- **Nothing gets stuck.** Leases expire, retries end in `dead`, and a suspended run must carry a timeout.
- **Auditable.** Every state change of every event is recorded: who, when, from, to and why.

## Quick start

You need JDK 21, Maven 3.9 and PostgreSQL 16.

```bash
cd proactive-agent

# 1. Database: starts a local cluster and creates role "inbox" and databases inbox_dev / inbox_test.
#    To use another PostgreSQL, skip this and set INBOX_JDBC_URL, INBOX_DB_USER and INBOX_DB_PASSWORD
#    for the service, and INBOX_TEST_JDBC_URL, INBOX_TEST_DB_USER and INBOX_TEST_DB_PASSWORD for the tests.
scripts/pg-local.sh

# 2. Build, run all tests and install the modules in the local Maven repository, where
#    scripts/run-worker.sh finds them (about a minute).
mvn install

# 3. Start the service with three callers: a producer, the runtime and an operator.
export INBOX_AUTH_CLIENTS_0_ID=order-bot  INBOX_AUTH_CLIENTS_0_SECRET=order-bot-secret \
       INBOX_AUTH_CLIENTS_0_TENANT=acme   INBOX_AUTH_CLIENTS_0_ROLES=PRODUCER
export INBOX_AUTH_CLIENTS_1_ID=runtime-1  INBOX_AUTH_CLIENTS_1_SECRET=runtime-secret \
       INBOX_AUTH_CLIENTS_1_ROLES=RUNTIME
export INBOX_AUTH_CLIENTS_2_ID=ops        INBOX_AUTH_CLIENTS_2_SECRET=ops-secret \
       INBOX_AUTH_CLIENTS_2_ROLES=OPERATOR
java -jar inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar
```

In a second terminal, write an event. `scripts/inbox-curl.sh` signs the request for you.

```bash
scripts/inbox-curl.sh order-bot order-bot-secret POST /v1/inbox/u42/events \
  '{"source":"biz_webhook","source_event_id":"order-1234-v7","type":"order.paid.v1",
    "merge_key":"order:1234","payload":{"order_id":"1234","amount":299}}'
# {"event_id":"01M7WZBC3TXC7X5N7KVK7Q9WXM","deduped":false,"status":"pending"}
# [HTTP 202]
```

Send it again and the answer is the same event with `"deduped":true`. Now let a worker process it.
The scripted worker claims a run, writes one reply into `target/outbox/` and acks.

```bash
scripts/run-worker.sh runtime-1 runtime-secret --worker-id worker-1 --max-runs 1
cat target/outbox/*.json
# {"run_id":"01M7WZBNDQ72G3BFC6VDVPF8RA","sequence":1,"channel":"im",...,
#  "text":"handled 1 event(s): order.paid.v1 order-1234-v7"}
```

Then read the event back with its whole history:

```bash
scripts/inbox-curl.sh order-bot order-bot-secret GET /v1/inbox/u42/events/01M7WZBC3TXC7X5N7KVK7Q9WXM
# {"event":{"event_id":"01M7WZBC3TXC7X5N7KVK7Q9WXM","status":"done",...},
#  "transitions":[{"to_status":"pending","reason":"accepted","actor":"client:order-bot",...},
#    {"from_status":"pending","to_status":"claimed","reason":"claim","actor":"worker:worker-1",...},
#    {"from_status":"claimed","to_status":"done","reason":"ack","actor":"worker:worker-1",...}]}
```

Health is at `/actuator/health` and metrics at `/actuator/prometheus`.

## Agent service

The quickest way to run it is Docker. This runs PostgreSQL, the Inbox and the agent service on one
machine, in demo mode, where a script plays the agent and no Anthropic key is needed;
[the tutorial](docs/en/tutorial.md) goes on from there to connecting an application and going live.

```bash
docker compose up -d --build --wait
examples/agent-api/quickstart.sh
```

Without Docker, the agent service needs the Inbox, with a client for the service:

```bash
# Terminal 1: the Inbox, with a client for the agent service (roles RUNTIME and PRODUCER, no tenant).
export INBOX_AUTH_CLIENTS_0_ID=agent-server INBOX_AUTH_CLIENTS_0_SECRET=agent-inbox-secret \
       INBOX_AUTH_CLIENTS_0_ROLES=RUNTIME,PRODUCER
java -jar inbox-server/target/inbox-server-0.1.0-SNAPSHOT-exec.jar

# Terminal 2: the agent service, in demo mode.
export AGENT_ADMIN_KEYS_0=change-me-admin-key-0123456789abcdef \
       AGENT_TOKENS_SECRETS_0=change-me-token-secret-0123456789abcdef \
       AGENT_INBOX_SECRET=agent-inbox-secret AGENT_CMA_MODE=demo
java -jar agent-server/target/agent-server-0.1.0-SNAPSHOT-exec.jar

# Terminal 3: an application, a user and a conversation, step by step.
examples/agent-api/quickstart.sh
```

With `AGENT_CMA_MODE=live` and `ANTHROPIC_API_KEY` set, the agents run on Claude Managed Agents;
check the account first with `scripts/cma-live-check.sh`. [Agent service](docs/en/agent-service.md)
explains how the service works and how to run it in production.

## Modules

| Module | What it is |
| --- | --- |
| `inbox-api` | Wire types shared by server and clients, canonical JSON, request signing |
| `inbox-server` | The service: write API, claim and run lifecycle, timers, replay, metrics |
| `inbox-client` | Plain Java client, `ScriptedWorker`, stub outboxes, a command-line worker |
| `openapi-testkit` | Test support: checks a service's HTTP traffic in tests against its OpenAPI document |
| `agent-cma` | The port to Claude Managed Agents: the interface the agent service uses, its adapter over the official Java SDK, and a scriptable in-process stand-in |
| `agent-api` | Wire types of the Agent API, shared by the agent service and its clients |
| `agent-server` | The agent service: the Agent API, turns on Claude Managed Agents, main agents and their workers' tasks, actions, schedules, updates, webhooks and credentials |

## Documentation

| Document | Content |
| --- | --- |
| [Architecture](docs/en/architecture.md) | The proactive agent as a whole and where the inbox sits |
| [Event Inbox design](docs/en/event-inbox-design.md) | How the inbox is built: invariants, data model, claim rules, decisions |
| [API](docs/en/api.md) | Every endpoint, request signing, error codes, the runtime contract, and where to get the OpenAPI document |
| [Integration guide](docs/en/integration.md) | How to connect a connector (MCP-based systems included) and an agent, step by step, with Java and Python examples |
| [Testing](docs/en/testing.md) | How to run the tests and which test proves which acceptance check |
| [Agent service](docs/en/agent-service.md) | How the agent service works, what it does when something breaks, and how to run it |
| [Agent API](docs/en/agent-api.md) | The guide for applications: concepts, authentication, conversations, events, actions, schedules, updates, credentials |
| [Main agent and workers](docs/en/main-agent.md) | The agent that hears of everything about a user and hands work to worker agents: the review, the decision and its reasons, how it works, and who lets a task start: the user's confirmation or standing authorisation |
| [Tutorial](docs/en/tutorial.md) | Run the service with Docker and connect an application step by step: back end, users' apps, events, approvals, live mode, going live |

## Configuration

Everything is under `inbox.*` and can be set by environment variable (`inbox.lease.duration` is
`INBOX_LEASE_DURATION`). The defaults are the numbers of the design.

| Property | Default | Meaning |
| --- | --- | --- |
| `INBOX_JDBC_URL`, `INBOX_DB_USER`, `INBOX_DB_PASSWORD` | local `inbox_dev` | Database |
| `INBOX_PORT` | `8080` | HTTP port |
| `inbox.auth.clients[n].id / secret / tenant / roles` | none | Callers. Roles: `PRODUCER`, `RUNTIME`, `OPERATOR`. No tenant means platform scope |
| `inbox.auth.clock-skew` | `300s` | Largest accepted difference between `X-Timestamp` and the server clock |
| `inbox.payload.max-bytes` | `65536` | Largest event payload |
| `inbox.lease.duration` / `max-duration` | `60s` / `30m` | One heartbeat's worth of lease; the cap per lease grant |
| `inbox.retry.max-attempts` | `5` | Attempts before a run is `dead` |
| `inbox.retry.backoff-base` / `backoff-cap` | `2s` / `5m` | Backoff after attempt n is base^n, capped |
| `inbox.retry.max-defer` | `30m` | For how long after a run was made a worker can hand it back without the attempt being counted |
| `inbox.batch.max-events` / `max-bytes` | `20` / `262144` | Batch caps |
| `inbox.lanes.<lane>.cap` | 1 / 2 / 4 | Concurrent runs per inbox for `interactive` / `background` / `batch` |
| `inbox.lanes.<lane>.quiet-window` / `max-wait` | `1500ms` / 4 × window | Batching window and its upper bound |
| `inbox.claim.poll-interval` / `max-wait` | `2s` / `30s` | Long-poll re-check interval and longest wait |
| `inbox.suspend.max-timeout` | `30d` | Longest suspend timeout |
| `inbox.timers.min-cron-period` | `60s` | Cron timers may not fire more often |
| `inbox.interrupt.keywords` | `stop, cancel, 停止, 取消` | Conversation texts that raise the interrupt flag |
| `inbox.scheduling.enabled` | `true` | Runs the background loops (sweepers, timer scheduler, gauges) |
| `inbox.wake.enabled` | `true` | In-process wake hints for long-polling claimers |

Lane caps, quiet windows and batch caps can be overridden per inbox through the operator API.

## Limits of this build

- Roadmap stage M2 is not built: per-client QPS limits, the per-inbox backlog limit (429), circuit
  breakers, sharding and consumption receipts.
- Wake hints are in-process. With several instances, a claimer on another instance finds new work on
  its next poll. Correctness is unaffected.
- A lost claim response delays that run by one lease interval (60 s).
- Finding work across inboxes scans the unclaimed events. That is fine at this size and is what
  sharding replaces.
- There are no retention or archiving jobs yet; tables only grow.
- Callers and their secrets come from configuration. A self-service registry belongs to M2.
- Timestamps come from the application clock, so nodes must be NTP-synchronised. Skew can only cause
  an early re-delivery; the run epoch prevents two owners.
