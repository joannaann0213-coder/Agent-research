-- Future work lives here and only here. A due timer produces an event through the normal write
-- statement; it never calls the runtime.
--
-- One row per occurrence. A deferred event (a write with a future ready_at) is a 'once' row that
-- carries the whole event. A cron series always has exactly one 'scheduled' row; firing it inserts
-- the next occurrence.
CREATE TABLE timer (
    timer_id        text        COLLATE "C" NOT NULL,
    fire_at         timestamptz NOT NULL,
    tenant_id       text        COLLATE "C" NOT NULL,
    inbox_id        text        COLLATE "C" NOT NULL,
    kind            text        NOT NULL CHECK (kind IN ('once', 'cron')),
    cron_expr       text,
    time_zone       text,
    timer_key       text,                              -- caller's idempotency key for timers made through the API
    state           text        NOT NULL CHECK (state IN ('scheduled', 'fired', 'missed', 'cancelled')),
    -- the event this timer will produce
    event_id        text        COLLATE "C",           -- pre-allocated id of a deferred event
    event_source    text        NOT NULL,
    event_type      text        NOT NULL,
    source_event_id text,                              -- NULL => derived: timer:<timer_id>:<fire_at epoch seconds>
    dedupe_kind     text,
    merge_key       text,
    lane            text        NOT NULL CHECK (lane IN ('interactive', 'background', 'batch')),
    resume_key      text,
    interrupt       boolean     NOT NULL DEFAULT false,
    payload         jsonb       NOT NULL,
    payload_hash    bytea,
    occurred_at     timestamptz,
    trace_id        text,
    client_id       text        NOT NULL,
    -- outcome
    fired_event_id  text,
    fired_at        timestamptz,
    catch_up        boolean     NOT NULL DEFAULT false, -- fired late, standing in for missed occurrences
    missed_count    int         NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL,
    updated_at      timestamptz NOT NULL,
    PRIMARY KEY (timer_id, fire_at),
    FOREIGN KEY (tenant_id, inbox_id) REFERENCES inbox (tenant_id, inbox_id),
    CONSTRAINT timer_cron_shape CHECK ((kind = 'cron') = (cron_expr IS NOT NULL AND time_zone IS NOT NULL))
);

CREATE INDEX timer_due ON timer (fire_at) WHERE state = 'scheduled';
CREATE UNIQUE INDEX timer_one_scheduled ON timer (timer_id) WHERE state = 'scheduled';
CREATE UNIQUE INDEX timer_deferred ON timer (tenant_id, inbox_id, source_event_id) WHERE source_event_id IS NOT NULL;
CREATE UNIQUE INDEX timer_by_key ON timer (tenant_id, inbox_id, timer_key) WHERE timer_key IS NOT NULL AND state = 'scheduled';
CREATE INDEX timer_event ON timer (event_id) WHERE event_id IS NOT NULL;
CREATE INDEX timer_by_inbox ON timer (tenant_id, inbox_id, timer_id);
