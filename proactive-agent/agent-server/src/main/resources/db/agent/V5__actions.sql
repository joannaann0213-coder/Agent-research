-- What waits for a person: a tool call that needs somebody's approval, or a question the agent asked
-- the user. The row is the authority on the outcome. It leaves 'pending' once, by a guarded update,
-- so a decision that was taken is never undone by a race with a deadline or with new input.
CREATE TABLE action (
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    action_id        text        COLLATE "C" NOT NULL,
    kind             text        NOT NULL CHECK (kind IN ('approval', 'question')),
    status           text        NOT NULL CHECK (status IN ('pending', 'approved', 'denied', 'answered', 'expired', 'withdrawn')),
    approver         text        NOT NULL CHECK (approver IN ('app', 'user')),
    merge_key        text        COLLATE "C" NOT NULL,   -- the line whose turn waits
    run_id           text        COLLATE "C" NOT NULL,   -- the run whose turn waits
    conversation_id  text        COLLATE "C" NOT NULL,   -- where the user sees it
    cma_session_id   text        NOT NULL,
    tool_use_id      text        NOT NULL,               -- the call that waits, in that session
    tool_server      text,                               -- approval: the MCP server
    tool_name        text,                               -- approval: the tool
    tool_input       jsonb,                              -- approval: what the call would do
    question         text,                               -- question: what the agent asks
    options          jsonb,                              -- question: answers the agent suggests
    answer           text,                               -- question: what the user answered
    note             text,                               -- why it was denied, expired or withdrawn
    resolved_by      text        CHECK (resolved_by IN ('app', 'user', 'service')),
    created_at       timestamptz NOT NULL,
    expires_at       timestamptz NOT NULL,
    resolved_at      timestamptz,
    PRIMARY KEY (app_id, user_id, action_id),
    UNIQUE (run_id, tool_use_id),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user,
    CHECK ((status = 'pending') = (resolved_at IS NULL))
);
CREATE INDEX action_pending ON action (app_id, user_id, conversation_id, created_at) WHERE status = 'pending';
CREATE INDEX action_run ON action (run_id);

-- A turn that waits for people has its run suspended in the Inbox: it is parked. New input on its
-- line closes it: it then finishes without waiting for anybody again, for the reason given here.
ALTER TABLE turn ADD COLUMN parked boolean NOT NULL DEFAULT false;
ALTER TABLE turn ADD COLUMN closing text;
-- The turn's time runs only while it works: a turn that resumes after waiting for people gets its time anew.
ALTER TABLE turn ADD COLUMN deadline_at timestamptz;
UPDATE turn SET deadline_at = created_at + interval '10 minutes';
ALTER TABLE turn ALTER COLUMN deadline_at SET NOT NULL;
-- What the turn works on, as found at its first delivery. A run that resumes is handed only what woke it.
ALTER TABLE turn ADD COLUMN work jsonb;
CREATE INDEX turn_line ON turn (app_id, user_id, merge_key) WHERE state = 'open';

-- The line a message belongs to. A user's message is input to its conversation's line, unless it is
-- the answer to a question that another line asked; the agent's messages belong to the line that
-- wrote them. A conversation's session is told about the messages of its conversation that other
-- lines exchanged with the user.
ALTER TABLE message ADD COLUMN merge_key text COLLATE "C";
UPDATE message SET merge_key = 'conv:' || conversation_id WHERE run_id IS NULL OR kind = 'reply';
UPDATE message m SET merge_key = t.merge_key FROM turn t WHERE m.merge_key IS NULL AND m.run_id = t.run_id;
UPDATE message SET merge_key = 'conv:' || conversation_id WHERE merge_key IS NULL;
ALTER TABLE message ALTER COLUMN merge_key SET NOT NULL;
-- A question is a message too, and so is the user's answer to one: both name the action.
ALTER TABLE message ADD COLUMN action_id text COLLATE "C";
ALTER TABLE message DROP CONSTRAINT message_kind_check;
ALTER TABLE message ADD CONSTRAINT message_kind_check CHECK (kind IN ('message', 'reply', 'notification', 'question'));
CREATE INDEX message_conversation ON message (app_id, user_id, conversation_id, seq);
