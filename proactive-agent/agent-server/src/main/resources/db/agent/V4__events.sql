-- What an application's own systems tell the agent about a user: an order shipped, a ticket was
-- answered, an alert fired. The data stays here; the Event Inbox only learns that there is an event.
CREATE TABLE app_event (
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    event_id         text        COLLATE "C" NOT NULL,   -- the caller's id, or one made here
    type             text        COLLATE "C" NOT NULL,
    subject          text        COLLATE "C" NOT NULL,   -- events with one subject form one line of work
    conversation_id  text        COLLATE "C" NOT NULL,   -- where the user is told, if the agent decides to
    data             jsonb       NOT NULL,
    -- A hash of everything the caller sent. A post with an id that was used before has to say the same.
    fingerprint      text        NOT NULL,
    deliver_at       timestamptz,
    created_at       timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, event_id),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user
);

-- How far a conversation's session has been told what was said to the user from outside it: the
-- position of the last notification it has read. A notification is written by another session, the
-- one that handled the event behind it, so the conversation's own session has to be told.
ALTER TABLE thread ADD COLUMN synced_seq bigint NOT NULL DEFAULT 0;

-- What the agent wrote at the end of a turn that no user reads. Kept for whoever has to find out
-- why a user was told something, or was not.
ALTER TABLE turn ADD COLUMN note text;

-- Counting a user's recent notifications, for the limit on them.
CREATE INDEX message_notification ON message (app_id, user_id, created_at) WHERE kind = 'notification';
