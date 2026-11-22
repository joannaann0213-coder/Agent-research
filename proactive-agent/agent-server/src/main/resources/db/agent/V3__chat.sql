-- A line of work with one user: a conversation. (Tasks that the application's own events start
-- become lines too.) Every line has a session of its own on the agent platform, and the Event Inbox
-- lets at most one run work on a line at a time, because the line's key is the merge key of its events.
CREATE TABLE thread (
    app_id             text        COLLATE "C" NOT NULL,
    user_id            text        COLLATE "C" NOT NULL,
    merge_key          text        COLLATE "C" NOT NULL,   -- conv:<conversation id>
    kind               text        NOT NULL CHECK (kind IN ('chat', 'task')),
    conversation_id    text        COLLATE "C",
    agent_key          text        COLLATE "C" NOT NULL,   -- fixed when the line starts
    -- The session on the agent platform. It can be replaced by a new one; each one is a generation.
    generation         integer     NOT NULL DEFAULT 0,     -- 0: no session yet
    cma_session_id     text,
    cma_agent_id       text,
    cma_agent_version  integer,
    -- The last event of the session that a finished turn has seen, on the platform's clock. The next
    -- turn reads the session from here on and so learns whether anything happened in between.
    last_event_id      text,
    last_event_at      timestamptz,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, merge_key),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user,
    FOREIGN KEY (app_id, agent_key) REFERENCES agent_profile
);

-- What the user and the agent said. seq is the position in the user's stream; it is handed out
-- under the lock of the user's row, so a reader that asks for "everything after seq N" never
-- misses a message that commits late.
CREATE TABLE message (
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    seq              bigint      NOT NULL,
    conversation_id  text        COLLATE "C" NOT NULL,
    message_id       text        COLLATE "C" NOT NULL,   -- the caller's id; for the agent's messages, derived from the run
    role             text        NOT NULL CHECK (role IN ('user', 'agent')),
    kind             text        NOT NULL CHECK (kind IN ('message', 'reply', 'notification')),
    text             text        NOT NULL,
    run_id           text        COLLATE "C",            -- for the agent's messages: the run that produced it
    created_at       timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, seq),
    -- One message per id: a retried post, and a run that stores its reply twice, both end here.
    UNIQUE (app_id, user_id, conversation_id, message_id),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user
);

-- One row per run of the Event Inbox that this service works on: where the turn stands, so that a
-- redelivery after a crash continues it instead of starting it again.
CREATE TABLE turn (
    run_id             text        COLLATE "C" PRIMARY KEY,
    app_id             text        COLLATE "C" NOT NULL,
    user_id            text        COLLATE "C" NOT NULL,
    merge_key          text        COLLATE "C" NOT NULL,
    -- The Inbox's epoch of the worker that owns the turn. Every write to this row and every result of
    -- the turn is conditional on it, so a worker that lost its lease cannot write after its successor.
    epoch              integer     NOT NULL,
    state              text        NOT NULL CHECK (state IN ('open', 'done', 'failed')),
    generation         integer,
    cma_session_id     text,
    -- The last event the session held before this turn sent its input. A message after it is this
    -- turn's own: that is how a redelivery knows whether the input was sent, without sending it again.
    boundary_set       boolean     NOT NULL DEFAULT false,
    boundary_event_id  text,
    boundary_at        timestamptz,
    input_event_id     text,
    outcome            text,
    error              text,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    finished_at        timestamptz,
    CHECK ((state = 'open') = (finished_at IS NULL))
);
CREATE INDEX turn_open ON turn (updated_at) WHERE state = 'open';
