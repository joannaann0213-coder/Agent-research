-- Agent service core schema.
--
-- Conventions, as in the Event Inbox
--   * Every key starts with app_id: applications cannot collide with or read each other.
--   * All timestamps are written from the application clock. There are no now() defaults.
--   * Ids sort bytewise (COLLATE "C").

-- An application that uses the service. Its id is also its tenant id in the Event Inbox.
CREATE TABLE app (
    app_id      text        COLLATE "C" PRIMARY KEY,
    name        text        NOT NULL,
    status      text        NOT NULL CHECK (status IN ('active', 'suspended')),
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL
);

-- API keys of an application. Only a hash of the secret is stored; the key is shown once, when it
-- is created.
CREATE TABLE api_key (
    key_id        text        COLLATE "C" PRIMARY KEY,
    app_id        text        COLLATE "C" NOT NULL REFERENCES app,
    secret_hash   bytea       NOT NULL,
    label         text,
    created_at    timestamptz NOT NULL,
    last_used_at  timestamptz,
    revoked_at    timestamptz
);
CREATE INDEX api_key_app ON api_key (app_id, created_at);

-- An end user of an application, created the first time the application mentions the user. The
-- user's id is also the id of the user's inbox in the Event Inbox.
CREATE TABLE app_user (
    app_id       text        COLLATE "C" NOT NULL REFERENCES app,
    user_id      text        COLLATE "C" NOT NULL,
    -- Every user token carries the epoch it was issued in. Raising the epoch revokes them all.
    token_epoch  integer     NOT NULL DEFAULT 0,
    -- The next position in the user's outbound stream. It is handed out under this row's lock, so
    -- that position order is commit order.
    next_seq     bigint      NOT NULL DEFAULT 1,
    created_at   timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id)
) WITH (fillfactor = 70);

-- Events on their way to the Event Inbox. A row is written in the same transaction as the row it is
-- about (a message, a schedule), and relayed afterwards. That replaces a write to two systems, which
-- can half fail, by one local commit and a delivery that is retried until the Inbox has the event.
-- The Inbox dedupes on the event's idempotency key, so relaying twice is harmless.
CREATE TABLE inbox_relay (
    relay_id         bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    request          jsonb       NOT NULL,   -- the body of the Inbox's "write an event" call
    status           text        NOT NULL CHECK (status IN ('pending', 'relayed', 'failed')),
    attempts         integer     NOT NULL DEFAULT 0,
    next_attempt_at  timestamptz NOT NULL,
    last_error       text,
    created_at       timestamptz NOT NULL,
    finished_at      timestamptz,
    CHECK ((status = 'pending') = (finished_at IS NULL))
);
CREATE INDEX inbox_relay_due ON inbox_relay (next_attempt_at, relay_id) WHERE status = 'pending';
