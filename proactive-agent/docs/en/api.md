# API

[中文](../zh/api.md)

The Event Inbox speaks JSON over HTTP under `/v1`. Producers write events and timers, the runtime
claims runs and ends them, and operators replay and configure. The examples on this page are real
responses captured from a running server. Which calls to make, in what order, when connecting a
connector or an agent is in the [Integration guide](integration.md).

## OpenAPI specification

The same API is described by an OpenAPI 3.0 document, for tools that generate clients or import
collections. A running service serves it at `GET /openapi.yaml`, without a signature. Its source is
[`inbox-server/src/main/resources/openapi/inbox.yaml`](../../inbox-server/src/main/resources/openapi/inbox.yaml).

```bash
curl http://localhost:8080/openapi.yaml
```

The document cannot drift away from the service: the build checks every request and every response
of the integration tests against it. See [Testing](testing.md#the-api-specification).

## Conventions

- Field names are `snake_case`. A field without a value is left out, not sent as `null`. JSON that the
  service stores as a document comes back as stored: an event's `payload`, and the `params` of an
  `inbox_config` audit entry, which lists a cap that was left out, and so went back to its default, as
  `null`.
- An unknown field is an error (400), so a typo in a field name is never silently ignored. That holds
  inside an inbox's `settings` too.
- Timestamps are ISO-8601 in UTC, for example `2026-11-20T17:27:39.671132Z`.
- Ids are ULIDs: 26 characters that sort by creation time.
- Errors are `application/problem+json` with a stable, machine-readable `code`.
- An `inbox_id` matches `[A-Za-z0-9][A-Za-z0-9_.:@-]{0,127}`.

## Authentication

Every `/v1` request is signed with HMAC-SHA256. Three headers carry the signature.

| Header | Value |
| --- | --- |
| `X-Client-Id` | The caller's client id |
| `X-Timestamp` | Unix time in seconds; accepted within 300 seconds of the server clock |
| `X-Signature` | `hmac-sha256=` followed by the hex signature |

The signature is computed with the client's secret over this string, with `\n` as the separator:

```text
v1
{timestamp}
{METHOD}
{path}?{query}
{hex sha256 of the body}
```

The method is upper-case, the query is included exactly as sent (left out with its `?` when there is
none), and an empty body hashes as the empty string. Binding the method, path and timestamp into the
signature stops a captured request from being replayed against another endpoint or at a later time.

```bash
timestamp=$(date +%s)
body_hash=$(printf '%s' "$body" | sha256sum | cut -d' ' -f1)
signature=$(printf 'v1\n%s\n%s\n%s\n%s' "$timestamp" "$method" "$path" "$body_hash" \
  | openssl dgst -sha256 -hmac "$secret" -hex | sed 's/^.*= *//')
```

`scripts/inbox-curl.sh` does exactly this, and `HmacSigner` is the Java implementation. A request body
larger than 128 KB is refused with 413 before it is parsed.

### Roles and tenants

| Role | Paths |
| --- | --- |
| `PRODUCER` | `/v1/inbox/**`: write events and timers, read back and cancel what it wrote |
| `RUNTIME` | `/v1/runs/**` |
| `OPERATOR` | `/v1/admin/**`; read access to `/v1/inbox/**` and to `GET /v1/runs/{run_id}`; cancel any timer |

A client that belongs to a tenant always acts inside it. A platform-scope client (configured without a
tenant) names the tenant: as the query parameter `tenant_id` on `/v1/inbox/**` paths and on admin `GET`
requests, and as the body field `tenant_id` on claim, replay, dead-letter and config requests. Calls on
a run need none, because the run id identifies the run. On a claim and on the audit log the tenant is
optional for a platform-scope client and absent means all tenants. An event, run or timer of another
tenant is reported as 404.

## Errors

```json
{"detail":"run 01M7X001ND6S05N2BHWQV128TR is no longer held under this epoch; stop working on it",
 "instance":"/v1/runs/01M7X001ND6S05N2BHWQV128TR/heartbeat","status":409,"title":"Conflict","code":"lease_lost"}
```

| Status | `code` | Meaning |
| --- | --- | --- |
| 400 | `invalid_request` | Malformed JSON, an unknown field, a missing or invalid value |
| 401 | `unauthenticated` | Missing, wrong or stale signature |
| 403 | `forbidden` | The client's role does not allow this endpoint, or it named another tenant |
| 404 | `not_found` | No such inbox, event, run or timer in this tenant |
| 405, 406, 415 | `invalid_request` | The path does not allow the method, the caller accepts no JSON, or the body was not sent as JSON |
| 409 | `idempotency_conflict` | The idempotency key was already used for a different event |
| 409 | `lease_lost` | The run is no longer held under this epoch. Stop working on it |
| 409 | `resume_key_in_use` | Another suspended run of this inbox waits for the same resume key |
| 409 | `conflict` | The object is not in a state that allows the action |
| 413 | `payload_too_large` | The payload is over 64 KB, or the request body is over 128 KB |
| 500 | `internal_error` | Unexpected failure |
| 503 | `database_unavailable` | The event store is unavailable. Retry with the same idempotency key |

## Producer API

### Write an event

`POST /v1/inbox/{inbox_id}/events`

| Field | Required | Meaning |
| --- | --- | --- |
| `source` | Yes | `biz_webhook`, `connector`, `timer`, `conversation`, `rule` or `agent_self` |
| `type` | Yes | Semantic type with namespace and version, for example `order.paid.v1`. At most 256 characters |
| `payload` | Yes | Any JSON value except `null`, at most 64 KB in canonical form |
| `source_event_id` | Recommended | Idempotency key, at most 256 characters. Without it the server fingerprints the event. The prefixes `fp1:` and `timer:` are reserved |
| `merge_key` | No | Batching key. Events with the same key are handled by one run at a time and can share a run |
| `lane` | No | `interactive`, `background` or `batch`. Defaults by source (see below) |
| `occurred_at` | No | When it happened at the source |
| `ready_at` | No | When it may run. Absent or past means now; a future value defers the event |
| `resume_key` | No | Set on a reply that should wake a suspended run |
| `interrupt` | No | Asks the run that is handling this key to yield. Defaults to a stop-word check on conversation messages |
| `trace_id` | No | Passed through to the runtime. Generated when absent |

The default lane is `interactive` for `biz_webhook`, `connector` and `conversation`, `background` for
`timer` and `agent_self`, and `batch` for `rule`.

```bash
scripts/inbox-curl.sh order-bot order-bot-secret POST /v1/inbox/u42/events \
  '{"source":"biz_webhook","source_event_id":"order-1234-v7","type":"order.paid.v1",
    "merge_key":"order:1234","payload":{"order_id":"1234","amount":299}}'
```

```json
{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","deduped":false,"status":"pending"}
```

**202 means accepted, not processed.** The event is durable and will be delivered at least once.

| Case | Status | Response |
| --- | --- | --- |
| New event | 202 | `deduped: false`, `status: pending` |
| Same key, same content | 202 | The earlier `event_id`, `deduped: true` and its current status |
| Same key, different content | 409 | `idempotency_conflict` |
| No `source_event_id` | 202 | `fingerprinted: true` and a `warning` that asks for an explicit key |
| Future `ready_at` | 202 | `status: scheduled`; the `event_id` is already final |

"Same content" compares the type and the canonical payload, so reordered keys and different spacing
are the same event. A producer should retry a 503 or a network error with the same key.

### Read events

`GET /v1/inbox/{inbox_id}/events/{event_id}` returns the event with its whole history, oldest first.

```json
{"event":{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","tenant_id":"acme","inbox_id":"u42","seq":1,
  "client_id":"order-bot","source":"biz_webhook","source_event_id":"order-1234-v7",
  "dedupe_kind":"provided","type":"order.paid.v1","payload":{"amount":299,"order_id":"1234"},
  "received_at":"2026-11-20T17:27:39.671132Z","ready_at":"2026-11-20T17:27:39.671132Z",
  "merge_key":"order:1234","lane":"interactive","interrupt":false,
  "trace_id":"01M7WZZSCQPP6KTQPVXCBECVHJ","status":"done","attempt":1,"max_attempts":5,
  "run_id":"01M7X001ND6S05N2BHWQV128TR","run_seq":1,"delivery_mode":"batch",
  "first_claimed_at":"2026-11-20T17:27:48.128723Z","finished_at":"2026-11-20T17:27:48.358564Z"},
 "transitions":[
  {"to_status":"pending","reason":"accepted","actor":"client:order-bot","at":"2026-11-20T17:27:39.671132Z"},
  {"from_status":"pending","to_status":"claimed","reason":"claim","actor":"worker:worker-1",
   "run_id":"01M7X001ND6S05N2BHWQV128TR","attempt":1,"at":"2026-11-20T17:27:48.128723Z"},
  {"from_status":"claimed","to_status":"done","reason":"ack","actor":"worker:worker-1",
   "run_id":"01M7X001ND6S05N2BHWQV128TR","attempt":1,"at":"2026-11-20T17:27:48.358564Z"}]}
```

`GET /v1/inbox/{inbox_id}/events` lists events in arrival order.

| Parameter | Meaning |
| --- | --- |
| `status` | `pending`, `claimed`, `done`, `failed` or `dead` |
| `source` | One of the source values |
| `from`, `to` | Range on the time received, `from <= t < to` |
| `limit` | 1 to 500, default 100 |

A producer sees the events it wrote. An operator sees every event of the tenant. A deferred event that
has not fired yet is read by id and has the status `scheduled`.

### Timers

`POST /v1/inbox/{inbox_id}/timers` creates a timer. A due timer writes a `timer.due` event into the
inbox; it never calls the runtime.

| Field | Meaning |
| --- | --- |
| `kind` | `once` or `cron` (required) |
| `fire_at` | When a `once` timer fires |
| `cron` | Five fields (minute first) or six (second first). At most one firing per 60 seconds |
| `time_zone` | IANA zone the cron is evaluated in, for example `Asia/Shanghai`. UTC by default |
| `timer_key` | Idempotency key. Creating a timer whose key is already scheduled returns that timer |
| `merge_key` | Batching key of the events the timer produces |
| `lane` | Lane of those events. `background` by default |
| `payload` | Carried into every event the timer produces. At most 32 KB |

```bash
scripts/inbox-curl.sh order-bot order-bot-secret POST /v1/inbox/u42/timers \
  '{"kind":"cron","cron":"0 9 * * 1-5","time_zone":"Asia/Shanghai","timer_key":"daily-report",
    "payload":{"report":"daily"}}'
```

```json
{"timer_id":"01M7X00EY2556QDKT50BS0CKKB","tenant_id":"acme","inbox_id":"u42","kind":"cron",
 "state":"scheduled","fire_at":"2026-11-23T01:00:00Z","cron":"0 9 * * 1-5","time_zone":"Asia/Shanghai",
 "timer_key":"daily-report","event_type":"timer.due","lane":"background","payload":{"report":"daily"},
 "created_at":"2026-11-20T17:28:01.726884Z"}
```

The answer is 201 for a new timer and 200 when the `timer_key` was already scheduled. The payload of a
`timer.due` event wraps the timer's payload:

```json
{"timer_id":"01M7X00EY2556QDKT50BS0CKKB","fire_at":"2026-11-23T01:00:00Z","kind":"cron",
 "catch_up":false,"missed_count":0,"payload":{"report":"daily"}}
```

- `GET /v1/inbox/{inbox_id}/timers/{timer_id}` returns the timer as its latest occurrence shows it, in
  the state `scheduled`, `fired` or `cancelled`. For a cron timer that is the next scheduled occurrence;
  whether a firing was a catch-up shows in the payload of the `timer.due` event it produced.
- `DELETE /v1/inbox/{inbox_id}/timers/{timer_id}` cancels the scheduled occurrence and answers 204. It
  answers 404 when nothing is scheduled. A timer that is firing right now wins the race and fires.
- A producer sees and cancels the timers it created. An operator sees every timer of the tenant and
  may cancel one; that cancellation is written to the audit log.

To defer one event, write it with a future `ready_at` instead. It then fires as itself, with its own
type and key.

## Runtime API

### Claim a run

`POST /v1/runs/claim`

| Field | Meaning |
| --- | --- |
| `worker_id` | Who is asking (required). Recorded as the lease owner and in the event history |
| `tenant_id` | Restricts the search to one tenant. Required when `inbox_id` is given |
| `inbox_id` | Claim from this inbox only. Absent means any inbox with work, oldest first |
| `lanes` | Restricts the claim to these lanes. Absent means all, highest priority first |
| `accepts_steer` | Whether same-key events that arrive later may be pulled into the run |
| `wait_ms` | How long to wait for work before answering 204. At most 30,000; 0 or absent means do not wait |

```json
{"run_id":"01M7X001ND6S05N2BHWQV128TR","epoch":1,"attempt":1,"kind":"start",
 "tenant_id":"acme","inbox_id":"u42","lane":"interactive","merge_key":"order:1234",
 "trace_id":"01M7WZZSCQPP6KTQPVXCBECVHJ","accepts_steer":true,
 "lease_expires_at":"2026-11-20T17:28:48.128723Z","lease_deadline_at":"2026-11-20T17:57:48.128723Z",
 "events":[{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","seq":1,"run_seq":1,"delivery_mode":"batch",
   "source":"biz_webhook","type":"order.paid.v1","source_event_id":"order-1234-v7",
   "received_at":"2026-11-20T17:27:39.671132Z","merge_key":"order:1234","interrupt":false,
   "payload":{"amount":299,"order_id":"1234"},"trace_id":"01M7WZZSCQPP6KTQPVXCBECVHJ"}]}
```

| `kind` | When | `events` holds |
| --- | --- | --- |
| `start` | A new run | Every event of the run |
| `redelivery` | The same run again, after a nack or a lease expiry | Every event of the run |
| `resume` with `resume_reason: event` | An event carrying the resume key arrived | The answering events |
| `resume` with `resume_reason: timeout` | The suspend deadline passed and the policy was `resume` | Every event of the run |

`delivery_mode` says how an event joined the run: `batch` (at the claim), `steer` or `resume`.

When there is nothing to claim the answer is 204 with two headers. This is normal scheduling, not an
error.

```text
HTTP/1.1 204
X-Inbox-Reason: window
X-Inbox-Retry-After-Ms: 1472
```

| `X-Inbox-Reason` | Meaning |
| --- | --- |
| `empty` | No unclaimed work |
| `busy` | There is work, but slots are taken or its merge key is being handled |
| `window` | The next batch is still inside its quiet window |
| `backoff` | The next run is waiting out its retry backoff |

`X-Inbox-Retry-After-Ms` is present when time alone will change the answer: a quiet window closes or a
retry backoff ends.

### Heartbeat

`POST /v1/runs/{run_id}/heartbeat` with `{"epoch":1}` renews the lease. The optional field
`accepts_steer` changes whether the run takes steered events.

```json
{"lease_expires_at":"2026-11-20T17:28:48.258144Z","extended":true,"steer_pending":0,"interrupt_requested":false}
```

| Field | Meaning |
| --- | --- |
| `lease_expires_at` | The lease now runs until this time |
| `extended` | `false` once the 30-minute cap is reached: the run will be reclaimed at `lease_expires_at` unless it ends first |
| `steer_pending` | Same-key events waiting to be pulled in with a steer call |
| `interrupt_requested` | An interrupt-flagged event is waiting behind this run; `interrupt_event_id` names it |

### Steer

`POST /v1/runs/{run_id}/steer` with `{"epoch":1,"after_run_seq":1}` binds waiting events of the run's
merge key to the run and returns every event of the run beyond `after_run_seq`. A lost response can
therefore be fetched again with the same cursor. Nothing joins a run that did not declare
`accepts_steer` or has no merge key; the events then wait for the run to end.

```json
{"events":[]}
```

### End a run

Every run ends with exactly one of these calls. Each answers with the run's state.

| Call | Body | Effect |
| --- | --- | --- |
| `POST /v1/runs/{run_id}/ack` | `{"epoch":1,"outcome":"done","last_run_seq":1}` | The run and the events it consumed are `done` |
| `POST /v1/runs/{run_id}/ack` | `{"epoch":1,"outcome":"failed","last_run_seq":1,"error":"…"}` | A final failure that is not retried |
| `POST /v1/runs/{run_id}/nack` | `{"epoch":1,"reason":"…","retry_after_ms":5000}` | The same run is issued again after a backoff; `dead` at the attempt limit |
| `POST /v1/runs/{run_id}/suspend` | `{"epoch":1,"resume_key":"approval:7788","timeout_seconds":86400,"on_timeout":"finish","last_run_seq":1}` | The run waits for an event that carries the resume key |

```json
{"run_id":"01M7X001ND6S05N2BHWQV128TR","status":"done","epoch":1,"attempt":1}
```

- `last_run_seq` is the highest `run_seq` the worker consumed. Later events of the run go back to the
  queue. Absent means every event.
- `retry_after_ms` overrides the exponential backoff and is capped at 5 minutes. The answer to a nack
  carries `next_attempt_at`.
- `resume_key` and `timeout_seconds` are required for a suspend. `on_timeout` is `finish` (default) or
  `resume`. The timeout is capped at 30 days.
- Repeating the same call with the same epoch returns the earlier outcome.

`GET /v1/runs/{run_id}` returns the run with the events bound to it.

## Runtime contract

These are the rules a runtime has to keep. `ScriptedWorker` in `inbox-client` follows them and can be
read as a reference.

1. **Carry the epoch.** Every call after the claim sends the `epoch` from the claim. On 409
   `lease_lost` stop at once and touch nothing: another worker owns the run.
2. **Heartbeat while working**, well inside the 60-second lease. Read the signals in the answer between
   two steps.
3. **End every run explicitly** with ack, nack or suspend. A run that just stops is delivered again
   when its lease expires.
4. **Expect redelivery.** Delivery is at-least-once. Send every message through an outbox keyed on
   `run_id + sequence`, where the n-th message of a run always has sequence n, on a first attempt, a
   redelivery and a resume alike. Make other side effects idempotent the same way.
5. **Send the cursor.** Ack and suspend carry `last_run_seq` so that an event steered in at the last
   moment is released, not lost.
6. **Suspend to wait.** Never hold a run open for a human. Save the state, suspend with a resume key
   and a timeout, and let the answer arrive as an event.
7. **Schedule follow-ups as events.** Write an event with a future `ready_at`, with source `agent_self`
   and an idempotency key such as `run_id + sequence`, and never sleep inside a run.

A claim without `wait_ms` is a poll. With `wait_ms` the server holds the request until work arrives, a
quiet window closes or a backoff ends, without holding a database connection.

## Operator API

### Replay

`POST /v1/admin/replay` puts finished events back in the queue. Exactly one selector is used.

| Selector | Fields |
| --- | --- |
| One event | `event_id` |
| An inbox and a time range | `inbox_id`, `from`, `to` |
| A source and a time range, across the tenant | `source`, `from`, `to` |

| Field | Meaning |
| --- | --- |
| `tenant_id` | The tenant (required for a platform-scope client) |
| `reason` | Why (required). Recorded with every replayed event and in the audit log |
| `operator` | The person doing it. Defaults to the client id |
| `force` | Required to replay an event that is `done` or `dead` |
| `dry_run` | Report what would be replayed without changing anything |

```json
{"replayed":[],"skipped":[{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK","status":"done","why":"needs_force"}],"dry_run":true}
```

`why` is `needs_force` or `in_flight`. The range is on the time received and is half-open,
`from <= t < to`. A range may match at most 10,000 events.

### Dead-letter an event

`POST /v1/admin/events/{event_id}/dead` with `{"tenant_id":"acme","reason":"…","operator":"alice"}`
takes a waiting event out of the queue and answers 204. Only an event that is waiting unclaimed can be
dead-lettered; anything else is 409 `conflict`. It can be replayed later with `force`.

### Inbox limits and status

`PUT /v1/admin/inboxes/{inbox_id}/config` replaces the inbox's overrides. A field that is left out goes
back to the service default.

| Field | Meaning |
| --- | --- |
| `cap_interactive`, `cap_background`, `cap_batch` | Concurrent runs per lane, 1 to 100 |
| `max_active_runs` | Inbox-wide cap on concurrent runs. 1 gives strict "one run per inbox" |
| `settings` | Per-lane `quiet_window_ms` and `max_wait_ms`, and batch caps `max_events` and `max_bytes` |

Any other key in `settings` is refused with 400, and so is a value out of range: `quiet_window_ms` and
`max_wait_ms` take 0 to 86,400,000 (one day), `max_events` and `max_bytes` at least 1. Batch caps apply
to the whole inbox, not per lane.

```json
{"tenant_id":"acme","max_active_runs":1,"operator":"alice",
 "settings":{"lanes":{"interactive":{"quiet_window_ms":3000}},"batch":{"max_events":20}}}
```

`GET /v1/admin/inboxes/{inbox_id}?tenant_id=acme` returns the configuration and what is waiting.

```json
{"tenant_id":"acme","inbox_id":"u42","max_active_runs":1,
 "settings":{"lanes":{"interactive":{"quiet_window_ms":3000}}},
 "pending":2,"retrying":0,"claimed":0,"dead":0,"active_runs":0,"queued_runs":0,"suspended_runs":0}
```

| Field | Counts |
| --- | --- |
| `pending` | Unclaimed events waiting to be picked up |
| `retrying` | Events bound to a run that is waiting to be issued again |
| `claimed` | Events held by an active or suspended run |
| `dead` | Events past their retry limit or dead-lettered by hand |

### Audit log

`GET /v1/admin/audit?tenant_id=acme&limit=100` lists operator actions, newest first. The actions are
`replay`, `manual_dead`, `inbox_config` and `timer_cancel`. The limit is at most 1,000.

```json
[{"audit_id":"01M7X00F23Z8EQ3KH18VZ0EM2P","tenant_id":"acme","actor_client_id":"ops","operator":"alice",
  "action":"replay","target":{"event_id":"01M7WZZSCQPP6KTQPVXCBECVHK"},
  "params":{"force":true,"reason":"customer asked for a re-run"},
  "result":{"skipped":0,"replayed":1},"at":"2026-11-20T17:28:01.853319Z"}]
```

## Health and metrics

| Path | Content |
| --- | --- |
| `/actuator/health` | Liveness and readiness, including the database |
| `/actuator/prometheus` | Metrics; see [Event Inbox design](event-inbox-design.md#observability) |

These two paths are not signed.

## Java client

`inbox-client` has a plain Java client with no framework dependency.

```java
InboxClient producer = new InboxClient(URI.create("http://127.0.0.1:8080"), "order-bot", "order-bot-secret");
WriteEventResponse accepted = producer.writeEvent("u42",
        WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "order.paid.v1")
                .sourceEventId("order-1234-v7")
                .mergeKey("order:1234")
                .payloadJson("{\"order_id\":\"1234\",\"amount\":299}")
                .build()).require();
```

A worker is a `ScriptedWorker` with a script that decides how each run ends.

```java
InboxClient runtime = new InboxClient(URI.create("http://127.0.0.1:8080"), "runtime-1", "runtime-secret");
Outbox outbox = new FileOutbox(Path.of("target/outbox"));
ScriptedWorker worker = new ScriptedWorker(runtime, outbox, "worker-1", context -> {
    context.heartbeat();
    context.send("im", "handled " + context.events().size() + " event(s)");
    return new ScriptedWorker.Decision.Done();
});
worker.runOnce();
```
