-- Event Inbox core schema.
--
-- Conventions
--   * Every key starts with tenant_id: tenants cannot collide with or read each other (invariant I8).
--   * All timestamps are written from the application clock. There are no now() defaults, so that
--     leases, windows and backoff can be driven deterministically in tests.
--   * Ids sort bytewise (COLLATE "C") because they are ULIDs.

CREATE FUNCTION inbox_reject_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '% on % is not allowed: %', TG_OP, TG_TABLE_NAME, TG_ARGV[0]
        USING ERRCODE = 'restrict_violation';
END;
$$;

-- One row per (tenant, inbox). Its row lock is the single-flight lock of the inbox: the write path
-- takes it to hand out seq, and every change to delivery state takes it first.
CREATE TABLE inbox (
    tenant_id        text        COLLATE "C" NOT NULL,
    inbox_id         text        COLLATE "C" NOT NULL,
    next_seq         bigint      NOT NULL,
    cap_interactive  smallint    CHECK (cap_interactive > 0),
    cap_background   smallint    CHECK (cap_background > 0),
    cap_batch        smallint    CHECK (cap_batch > 0),
    max_active_runs  smallint    CHECK (max_active_runs > 0),   -- NULL = no inbox-wide cap; 1 = one run per inbox
    settings         jsonb       NOT NULL DEFAULT '{}'::jsonb,  -- per-lane quiet_window_ms / max_wait_ms, batch caps
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL,
    PRIMARY KEY (tenant_id, inbox_id)
) WITH (fillfactor = 70);

-- One execution. A run is minted at its first claim and owns its events until it ends: a nack or a
-- lease expiry re-issues the same run (same id, same events), which is what lets a downstream outbox
-- dedupe on run_id + sequence.
CREATE TABLE run (
    run_id              text        COLLATE "C" PRIMARY KEY,
    tenant_id           text        COLLATE "C" NOT NULL,
    inbox_id            text        COLLATE "C" NOT NULL,
    lane                text        NOT NULL CHECK (lane IN ('interactive', 'background', 'batch')),
    merge_key           text,
    head_seq            bigint      NOT NULL,
    status              text        NOT NULL CHECK (status IN ('active', 'queued', 'suspended', 'done', 'failed', 'dead')),
    slot_no             smallint,
    epoch               int         NOT NULL,          -- +1 on every lease grant; fences out stale workers
    attempt             int         NOT NULL,          -- failure budget; a resume does not spend it
    max_attempts        int         NOT NULL,
    lease_owner         text,
    lease_started_at    timestamptz,
    lease_expires_at    timestamptz,
    lease_deadline_at   timestamptz,                   -- lease_started_at + maximum lease duration
    next_attempt_at     timestamptz,                   -- when a queued run may be re-issued (retry backoff)
    queued_reason       text,                          -- nack, lease_expired or suspend_timeout
    accepts_steer       boolean     NOT NULL DEFAULT false,
    next_run_seq        int         NOT NULL,          -- next ordinal for an event joining this run
    resume_key          text,
    suspend_deadline_at timestamptz,
    on_timeout          text        CHECK (on_timeout IN ('finish', 'resume')),
    resume_reason       text,
    trace_id            text        NOT NULL,
    eligible_at         timestamptz,                   -- when the first batch became claimable (latency metric)
    end_reason          text,
    last_error          text,
    created_at          timestamptz NOT NULL,
    updated_at          timestamptz NOT NULL,
    finished_at         timestamptz,
    FOREIGN KEY (tenant_id, inbox_id) REFERENCES inbox (tenant_id, inbox_id),
    CONSTRAINT run_active_shape    CHECK ((status = 'active')    = (slot_no IS NOT NULL AND lease_expires_at IS NOT NULL)),
    CONSTRAINT run_suspended_shape CHECK ((status = 'suspended') = (resume_key IS NOT NULL AND suspend_deadline_at IS NOT NULL)),
    CONSTRAINT run_queued_shape    CHECK ((status = 'queued')    = (next_attempt_at IS NOT NULL))
);

-- The three single-flight limits, enforced by the database as a backstop to the claim planner.
CREATE UNIQUE INDEX run_slot   ON run (tenant_id, inbox_id, lane, slot_no) WHERE status = 'active';
CREATE UNIQUE INDEX run_key    ON run (tenant_id, inbox_id, merge_key)     WHERE status = 'active' AND merge_key IS NOT NULL;
CREATE UNIQUE INDEX run_resume ON run (tenant_id, inbox_id, resume_key)    WHERE status = 'suspended';
CREATE INDEX run_lease   ON run (lease_expires_at)    WHERE status = 'active';
CREATE INDEX run_suspend ON run (suspend_deadline_at) WHERE status = 'suspended';
CREATE INDEX run_queued  ON run (next_attempt_at) INCLUDE (tenant_id, inbox_id, lane) WHERE status = 'queued';
CREATE INDEX run_open    ON run (tenant_id, inbox_id) WHERE status IN ('active', 'queued', 'suspended');

-- The source of truth. Content columns are immutable (trigger below); only delivery state changes.
CREATE TABLE event (
    event_id         text        COLLATE "C" PRIMARY KEY,
    tenant_id        text        COLLATE "C" NOT NULL,
    inbox_id         text        COLLATE "C" NOT NULL,
    seq              bigint      NOT NULL,             -- handed out under the inbox row lock: seq order = commit order
    client_id        text        NOT NULL,
    source           text        NOT NULL CHECK (source IN ('biz_webhook', 'connector', 'timer', 'conversation', 'rule', 'agent_self')),
    source_event_id  text        NOT NULL,             -- idempotency key (provided, or a fingerprint)
    dedupe_kind      text        NOT NULL CHECK (dedupe_kind IN ('provided', 'fingerprint')),
    type             text        NOT NULL,
    payload          jsonb       NOT NULL,
    payload_hash     bytea       NOT NULL,             -- sha256(type + canonical payload): the 409 check
    payload_bytes    int         NOT NULL CHECK (payload_bytes >= 0),
    occurred_at      timestamptz,
    received_at      timestamptz NOT NULL,
    ready_at         timestamptz NOT NULL,
    merge_key        text,
    lane             text        NOT NULL CHECK (lane IN ('interactive', 'background', 'batch')),
    resume_key       text,
    interrupt        boolean     NOT NULL DEFAULT false,
    trace_id         text        NOT NULL,
    timer_id         text,
    -- delivery state
    status           text        NOT NULL CHECK (status IN ('pending', 'claimed', 'done', 'failed', 'dead')),
    attempt          int         NOT NULL DEFAULT 0,
    max_attempts     int         NOT NULL DEFAULT 5,
    run_id           text        COLLATE "C" REFERENCES run (run_id),
    run_seq          int,
    delivery_mode    text        CHECK (delivery_mode IN ('batch', 'steer', 'resume')),
    first_claimed_at timestamptz,
    finished_at      timestamptz,
    last_error       text,
    FOREIGN KEY (tenant_id, inbox_id) REFERENCES inbox (tenant_id, inbox_id),
    CONSTRAINT event_idempotency  UNIQUE (tenant_id, inbox_id, source_event_id),   -- invariant I2
    CONSTRAINT event_seq          UNIQUE (tenant_id, inbox_id, seq),
    -- A future time never enters this table: it waits in "timer" (invariant I6).
    CONSTRAINT event_ready_now    CHECK (ready_at <= received_at),
    CONSTRAINT event_claimed_run  CHECK (status <> 'claimed' OR run_id IS NOT NULL)
);

-- "Fresh" events: accepted and not yet owned by any run. This is the ready queue.
CREATE INDEX event_fresh ON event (tenant_id, inbox_id, seq)
    INCLUDE (lane, merge_key, resume_key, interrupt, payload_bytes, received_at)
    WHERE status = 'pending' AND run_id IS NULL;
CREATE INDEX event_run     ON event (run_id, run_seq) WHERE run_id IS NOT NULL;
CREATE INDEX event_by_time ON event (tenant_id, inbox_id, received_at);
CREATE INDEX event_by_src  ON event (tenant_id, source, received_at);
CREATE INDEX event_dead    ON event (tenant_id, lane) WHERE status = 'dead';

-- Invariant I1: an event is a fact and is never rewritten. Status updates do not name these columns.
CREATE TRIGGER event_immutable
    BEFORE UPDATE OF event_id, tenant_id, inbox_id, seq, client_id, source, source_event_id, dedupe_kind,
                     type, payload, payload_hash, payload_bytes, occurred_at, received_at, ready_at,
                     merge_key, lane, resume_key, interrupt, trace_id, timer_id
    ON event
    FOR EACH ROW EXECUTE FUNCTION inbox_reject_change('event content is immutable');

-- Invariant I7: who moved an event, when, from where to where, and why. Append-only.
CREATE TABLE event_transition (
    transition_id bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id      text        COLLATE "C" NOT NULL,
    tenant_id     text        COLLATE "C" NOT NULL,
    inbox_id      text        COLLATE "C" NOT NULL,
    from_status   text,
    to_status     text        NOT NULL,
    reason        text        NOT NULL,   -- accepted, claim, redelivery, steer, resume, ack, fail, nack, lease_expired,
                                          -- attempts_exhausted, suspended, suspend_timeout, released, replay, manual_dead
    actor         text        NOT NULL,   -- client:<id>, worker:<id>, system:<job>, operator:<name>
    run_id        text,
    attempt       int,
    detail        jsonb,
    at            timestamptz NOT NULL
);
CREATE INDEX event_transition_event ON event_transition (event_id, transition_id);

CREATE TRIGGER event_transition_append_only
    BEFORE UPDATE OR DELETE ON event_transition
    FOR EACH ROW EXECUTE FUNCTION inbox_reject_change('the transition history is append-only');
