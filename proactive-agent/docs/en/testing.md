# Testing

[中文](../zh/testing.md)

Every acceptance check of the design (F1 to F10) and every scenario (A to E) has an automated test.
Database tests run against a real PostgreSQL, and the end-to-end tests go through real HTTP with the
same client a producer or a runtime would use.

## Running the tests

You need JDK 21, Maven 3.9 and PostgreSQL 16.

```bash
cd proactive-agent
# Start PostgreSQL and create role "inbox" and database "inbox_test".
scripts/pg-local.sh
# Unit tests, then integration and end-to-end tests.
mvn verify
```

The whole run takes about a minute. To use another database, set these variables. The tests empty
every Inbox table in that database, so never point them at one that holds data.

| Variable | Default |
| --- | --- |
| `INBOX_TEST_JDBC_URL` | `jdbc:postgresql://127.0.0.1:5432/inbox_test` |
| `INBOX_TEST_DB_USER` | `inbox` |
| `INBOX_TEST_DB_PASSWORD` | `inbox` |

There is no Docker in the loop. The tests need a PostgreSQL they can reach, nothing else.

```bash
# Unit tests only, no database.
mvn -pl inbox-server -am test
# One integration test class.
mvn -pl inbox-server -am verify -Dit.test=F5BatchingIT \
    -Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

## Kinds of test

| Kind | Naming | Runs with | What it is |
| --- | --- | --- | --- |
| Unit | `*Test` | Surefire, no database | Pure logic: the claim planner, cron schedules, the reclaim gate, canonical JSON, signing, the stub outboxes |
| Integration | `*IT` | Failsafe, PostgreSQL | The real service on a random port, called over HTTP |
| End to end | `*E2E` | Failsafe, PostgreSQL | A scenario from the design, with `ScriptedWorker` standing in for the agent |
| Measurement | `WriteLatencyPerf` | On request only | Write latency against the design target |
| Documentation | `DocsParityTest` | Surefire | The English and Chinese documents have the same structure and working links |
| Contract | `OpenApiContractIT`, `OpenApiSchemaTest` | Failsafe and Surefire | The OpenAPI document against the running service and against the wire types |

## How the tests stay deterministic

- **The clock only moves when a test moves it.** The service takes time from an injected clock and
  passes it into SQL, so leases, quiet windows, backoff and timers are tested by advancing a
  `MutableClock`, not by sleeping.
- **Background loops are off.** The lease sweeper, the suspend sweeper and the timer scheduler are
  ticked by hand with `runOnce()`, one pass at a time.
- **Each test starts from empty tables.**
- **Invariants are checked after every database test**, whatever the test was about (see below).
- **Round trips are counted.** `StatementCounter` counts SQL statements, which is how "the happy path
  of a write is one statement" is asserted.

A few tests use real time on purpose: the long-poll and wake tests, the 50-thread single-flight test
and scenario D, which kills a real process.

### Invariants checked after every test

| Check | Protects |
| --- | --- |
| A claimed event belongs to an active or suspended run | No event is stuck with a dead owner |
| A pending event that is bound to a run belongs to a queued run | A retry keeps its events |
| Exactly the finished events carry `finished_at` | State and timestamps agree |
| Every event's history is an unbroken chain starting from nothing | Every transition is legal |
| The last recorded transition is the event's current state | State and history agree |
| At most one active run per merge key in an inbox | Single flight |
| The database reports no new deadlock | The lock order holds |

## The API specification

The OpenAPI document is checked against the service, so it cannot describe an API that does not
exist. A change to the API that is not made in the document fails the build.

| Check | Where | What fails the build |
| --- | --- | --- |
| Traffic | After every integration test | A response that does not match its schema, a response field the schema does not list, a status the operation does not list, an accepted request that breaks the document |
| Coverage | `OpenApiContractIT`, which runs last | An operation that no test called; a listed status that no test produced; a controller mapping that the document lacks, or the reverse |
| Inventory | `OpenApiSchemaTest` | A schema whose fields differ from its wire type in `inbox-api`; an enum whose values differ from the code |
| Soundness | `OpenApiSchemaTest` | A reference to a missing component, a component nothing uses, an operation without an `operationId`, an example that does not match its own schema |

Two limits are deliberate. A request that the service refuses is not checked against the document,
because a wrong request is usually the point of such a test; the response to it is checked. And
coverage does not ask for 400, 401 and 403 on every operation, because shared layers produce them.

The checks live in the module `openapi-testkit`. It knows nothing about the Inbox and is reused for
the other services of this repository.

## Acceptance checks

| ID | Acceptance check | Tests |
| --- | --- | --- |
| F1 | A write is accepted with no worker running and with a broken wake channel; three retries give one event; the happy path is one SQL statement; a database failure is an explicit 503 | `F1WriteApiIT`, `F1WakeFailureIT`, `AuthIT`, `SeqCommitOrderIT` |
| F2 | One webhook sent 10 times, in sequence and from 10 threads, is 1 event, 1 run and 1 reply; a changed payload is a 409; reordered JSON is the same event; a missing key is fingerprinted | `F2DedupeIT`, `ScenarioAE2E` |
| F3 | Every transition records who, when, from, to and why; a nack retries with backoff and ends in `dead`; after a worker is killed the same run is delivered again and no event is lost; a call with a stale epoch gets 409 | `F3StateMachineIT`, `EventImmutabilityIT`, `ScenarioDE2E` |
| F4 | A future event exists only as a timer; with several schedulers and a rollback every timer fires exactly once; after an outage a cron fires one catch-up event | `F4TimerIT`, `CronScheduleTest`, `ScenarioEE2E` |
| F5 | Five messages in a row are one run with five events and one reply; a batch is capped at 20 events and 256 KB; `max_wait` bounds a trickle | `F5BatchingIT`, `ClaimPlannerTest`, `ScenarioBE2E` |
| F6 | 50 threads claim and ack on one inbox: never more than one active run, head sequence strictly increasing, every event done once; repeated in strict mode | `F6SingleFlightIT` |
| F7 | One user's long task does not delay another user; a background run does not block the same user's new message; the same merge key never runs twice at once | `F7LaneIT` |
| F8 | With wake hints off an event is still claimed within 10 s; a waiting claim is woken by a write; a lease renews only up to 30 minutes and is then reclaimed | `F8WakeIT`, `F8WakeDisabledIT`, `ReclaimGateTest` |
| F9 | Steer, followup, collect and interrupt each behave as specified; an ignored interrupt falls back to followup; random interleavings never lose an event; suspend frees the inbox and the answer resumes the same run | `F9DeliveryModesIT`, `F9RandomInterleavingsIT`, `SuspendResumeIT`, `ScenarioCE2E` |
| F10 | Replay with and without `force`; audit rows carry the operator and the reason; one call returns an event's whole history; another tenant's objects are not found | `F10ReplayAuditIT`, `InboxAdminIT`, `TenantIsolationIT`, `MetricsIT` |
| F11 | Quotas, backpressure and circuit breaking | Not built (roadmap stage M2), so not tested |

## Scenarios

| Scenario | Test | What it proves |
| --- | --- | --- |
| A. A business alert, one reply | `ScenarioAE2E` | Write, wake, claim, heartbeats that keep the run alive past 60 s, one outbox message, ack; the same `trace_id` at the write, the claim and the outbox |
| B. Five user messages in a row | `ScenarioBE2E` | One run, one reply that reflects all five |
| C. Suspend for approval | `ScenarioCE2E` | The run asks, suspends and continues on approval; a rejection is an answer too; with no answer the run ends silently; a crash after applying the change neither applies nor announces it twice |
| D. A worker crash | `ScenarioDE2E` | A worker in its own JVM is force-killed in the middle of a run. After the lease expires the same `run_id` is delivered again with a higher epoch, and the reply is sent once |
| E. A timed follow-up | `ScenarioEE2E` | Two hours later the agent sends the reminder, or stays silent because the reply already came |

## Invariants

| Invariant | Tests |
| --- | --- |
| I1 One write entry, append only | `EventImmutabilityIT`, `F1WriteApiIT` |
| I2 One event per idempotency key | `F2DedupeIT` |
| I3 Single flight | `F6SingleFlightIT`, `F7LaneIT`, the check after every test |
| I4 A run is not 1:1 with an event | `F5BatchingIT` |
| I5 At-least-once, idempotent consumers | `F3StateMachineIT`, `ScenarioDE2E`, `OutboxTest` |
| I6 Future times never enter the ready queue | `F4TimerIT` |
| I7 Immutable events, auditable transitions | `EventImmutabilityIT`, `F3StateMachineIT`, the check after every test |
| I8 Tenant isolation | `TenantIsolationIT`, `AuthIT` |

## Write latency

The design asks for a write p99 of at most 50 ms. `WriteLatencyPerf` measures it. It is not part of
the normal build, because the result depends on the machine and above all on how fast its disk confirms
a commit.

```bash
mvn -pl inbox-server -am verify -Dit.test=WriteLatencyPerf \
    -Dfailsafe.failIfNoSpecifiedTests=false -Dsurefire.failIfNoSpecifiedTests=false
```

It prints lines that start with `WRITE LATENCY`. The times are taken by the client over loopback HTTP,
so they include signing, the HTTP round trip and JSON on both sides. The result on the development VM
is in [Event Inbox design](event-inbox-design.md#targets-and-measurements).

## Trying it by hand

The README's quick start is the manual smoke test: start the service, write a signed event with
`scripts/inbox-curl.sh`, let `scripts/run-worker.sh` claim and ack it, and read the event's history
back.

## Against the real service

The agent service's tests run on a stand-in for Claude Managed Agents. Three scripts run against the
real service instead, with an API key. They were last run in November 2026, with `claude-opus-5-5`.

| Script | What it checks | Result |
| --- | --- | --- |
| `scripts/cma-live-check.sh` | 22 behaviours of the platform that the agent service depends on and that the documentation leaves open | The 21 that it checked then: all as assumed. The 22nd, that a session says what every request to the model used, was added afterwards |
| `scripts/agent-live-smoke.sh` | A conversation, an event that becomes a notification, a question that the user answers | All passed |
| `scripts/main-agent-live.sh` | A main agent and a worker: a task that the user asks for, an event and a schedule that the application hands to the worker, work that the main agent offers and the user confirms, how forward it is, and the user writing while a report is on its way. Three steps were added afterwards: the user writing again before the answer is there, two workers at work at the same time, and what the turns used | The steps it had then: all passed. The three later ones have not been run against the real service |

The first run of the platform check found four things that the stand-in had wrong. The agent service
relied on none of them. The stand-in and its contract tests now do what the service does:

- The stream shows a message once, when the session takes it up and after it has said that it is
  running. The stored list has the message at that place too.
- An answer to a call that no longer waits, because it was answered or its turn was interrupted, is
  accepted and changes nothing. A confirmation makes the session show itself running and idle once
  more; the result of a custom tool shows nothing.
- An interrupt gives the calls that wait an error as their result before the session goes idle.

With the model, a reply of the main agent took 5 to 6 seconds in the service, and reached the user 7
to 8 seconds after they wrote, the Inbox's quiet window included. A turn in the background took 5 to
7 seconds, a worker's turn about 5. In two runs a turn in the background gave way to a message of the
user four times: three times the user's turn had taken its news and it ended without the model, and
once it started over and repeated its calls, none of which was made twice.

Not run against the real service: a worker that waits for an approval. That needs an MCP server that
the platform can reach. Nor the counting of tokens, which came later: the runs showed that a session
stores an event with the usage after every request to the model, which is what the count reads, and
the official SDK has the event's type; the numbers themselves have only been checked on the stand-in.

## What is not tested

- Roadmap stage M2 (F11) is not built.
- Nothing was run in Docker or on more than one service instance. The background loops are written to
  be safe on every instance. The tests run several timer schedulers at once and race the suspend
  sweeper against a claiming worker; the lease sweeper is only interleaved with worker calls step by
  step. All of this happens inside one process.
- There is no production-scale load test. The 50-thread single-flight test and the latency measurement
  are the largest loads applied.
