-- What happened for a user that the application wants to hear about: a message, an action that
-- waits for somebody or was decided, a run that failed. The log behind the stream and the webhook.
-- seq is the position in the user's log. It is handed out under the lock of the user's row, so a
-- reader that has seen position N never sees a lower one appear later.
ALTER TABLE app_user ADD COLUMN next_event_seq bigint NOT NULL DEFAULT 1;

CREATE TABLE outbound_event (
    app_id      text        COLLATE "C" NOT NULL,
    user_id     text        COLLATE "C" NOT NULL,
    seq         bigint      NOT NULL,
    event_id    text        COLLATE "C" NOT NULL UNIQUE,
    type        text        NOT NULL,
    data        jsonb       NOT NULL,
    created_at  timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, seq),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user
);

-- A browser's EventSource cannot send a header. It opens the stream with a ticket instead: good
-- once, for a minute, for one user. Only the ticket's hash is kept.
CREATE TABLE stream_ticket (
    ticket_hash  text        PRIMARY KEY,
    app_id       text        COLLATE "C" NOT NULL,
    user_id      text        COLLATE "C" NOT NULL,
    expires_at   timestamptz NOT NULL,
    used_at      timestamptz
);

-- Where an application wants the log of all its users delivered. One endpoint per application.
CREATE TABLE webhook (
    app_id           text        COLLATE "C" PRIMARY KEY REFERENCES app,
    url              text        NOT NULL,
    events           jsonb,                      -- the types it gets; all when null
    secret           text        NOT NULL,       -- signs every delivery
    state            text        NOT NULL CHECK (state IN ('active', 'disabled')),
    disabled_reason  text,
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL
);

CREATE TABLE webhook_delivery (
    delivery_id      bigserial   PRIMARY KEY,
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    event_id         text        COLLATE "C" NOT NULL,
    status           text        NOT NULL CHECK (status IN ('pending', 'delivered', 'failed', 'skipped')),
    attempts         integer     NOT NULL DEFAULT 0,
    next_attempt_at  timestamptz,
    last_status      integer,
    last_error       text,
    created_at       timestamptz NOT NULL,
    finished_at      timestamptz,
    UNIQUE (app_id, event_id)
);
CREATE INDEX webhook_delivery_due ON webhook_delivery (next_attempt_at) WHERE status = 'pending';
