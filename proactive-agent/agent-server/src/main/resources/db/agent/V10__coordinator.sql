-- A main agent hands work to workers. Each piece of work it hands out is a task: a line of work of
-- its own, with a session of its own on the agent platform, on which the worker's agent runs. What
-- the two tell each other goes through this service, as notes of the task, and each note wakes the
-- other side through the Event Inbox like any other input.

-- A line can now also be the line of a task: work:<task id>. Its conversation_id is the conversation
-- of the main agent that delegated it, which is where people see what the worker waits for.
ALTER TABLE thread DROP CONSTRAINT thread_kind_check;
ALTER TABLE thread ADD CONSTRAINT thread_kind_check CHECK (kind IN ('chat', 'task', 'work'));

CREATE TABLE delegated_task (
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    task_id          text        COLLATE "C" NOT NULL,
    hub_key          text        COLLATE "C" NOT NULL,   -- the main agent's line: conv:<conversation id>
    conversation_id  text        COLLATE "C" NOT NULL,
    role             text        COLLATE "C" NOT NULL,   -- the role it was delegated to
    agent_key        text        COLLATE "C" NOT NULL,   -- the agent that works on it
    title            text        NOT NULL,
    -- queued: the worker has not taken up what the main agent last sent. working: a turn of the worker
    -- is under way. idle: the worker reported and waits. closed: over.
    status           text        NOT NULL CHECK (status IN ('queued', 'working', 'idle', 'closed')),
    closed_by        text        CHECK (closed_by IN ('agent', 'user', 'app', 'service')),
    closed_reason    text,
    next_note_seq    integer     NOT NULL DEFAULT 1,
    -- The call that delegated it: the run of the main agent's turn, and the call's place among that
    -- turn's calls of the tool. A run that is delivered again finds the task it made the first time.
    created_by_run   text        COLLATE "C" NOT NULL,
    created_by_call  integer     NOT NULL,
    created_at       timestamptz NOT NULL,
    updated_at       timestamptz NOT NULL,
    closed_at        timestamptz,
    PRIMARY KEY (app_id, user_id, task_id),
    UNIQUE (created_by_run, created_by_call),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user,
    CHECK ((status = 'closed') = (closed_at IS NOT NULL))
);
CREATE INDEX delegated_task_open ON delegated_task (app_id, user_id, hub_key, created_at) WHERE status <> 'closed';
CREATE INDEX delegated_task_user ON delegated_task (app_id, user_id, created_at);

-- What the main agent and the worker told each other about a task, in order. A note is written in
-- the transaction that also queues the Inbox event which wakes the side it is for.
CREATE TABLE task_note (
    app_id      text        COLLATE "C" NOT NULL,
    user_id     text        COLLATE "C" NOT NULL,
    task_id     text        COLLATE "C" NOT NULL,
    seq         integer     NOT NULL,
    direction   text        NOT NULL CHECK (direction IN ('to_worker', 'to_main')),
    -- to_worker: assignment, message. to_main: report (the worker's), failure and closed (the service's).
    kind        text        NOT NULL CHECK (kind IN ('assignment', 'message', 'report', 'failure', 'closed')),
    text        text        NOT NULL,
    -- Derived from the run and the call that wrote the note, so that writing it twice is writing it once.
    note_key    text        COLLATE "C" NOT NULL,
    run_id      text        COLLATE "C",
    -- When a turn of the side it is for ended having been given it.
    read_at     timestamptz,
    created_at  timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, task_id, seq),
    UNIQUE (app_id, user_id, task_id, note_key),
    FOREIGN KEY (app_id, user_id, task_id) REFERENCES delegated_task,
    CHECK ((direction = 'to_worker') = (kind IN ('assignment', 'message')))
);
