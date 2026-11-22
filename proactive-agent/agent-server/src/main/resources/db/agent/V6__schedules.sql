-- What the agent is to do at a given time: a schedule the application set up (once, or on a cron
-- rule in the user's time zone), or a follow-up the agent set itself. The Event Inbox keeps the
-- time, as a timer whose events carry the schedule's id; what to do stays here.
CREATE TABLE schedule (
    app_id           text        COLLATE "C" NOT NULL,
    user_id          text        COLLATE "C" NOT NULL,
    schedule_id      text        COLLATE "C" NOT NULL,
    created_by       text        NOT NULL CHECK (created_by IN ('app', 'agent')),
    kind             text        NOT NULL CHECK (kind IN ('once', 'cron')),
    name             text,                               -- the application's: a short name
    instructions     text,                               -- the application's: what the agent is to do
    note             text,                               -- the agent's: its note to itself
    chain            integer     NOT NULL DEFAULT 0,     -- the agent's: how many follow-ups in a row this is
    merge_key        text        COLLATE "C" NOT NULL,   -- the line it runs on
    agent_key        text        COLLATE "C" NOT NULL,
    conversation_id  text        COLLATE "C" NOT NULL,   -- where the user is told, if the agent tells them
    fire_at          timestamptz,                        -- once
    cron             text,                               -- cron
    time_zone        text,                               -- cron: the zone the rule is read in
    fingerprint      text        NOT NULL,               -- everything the caller asked for: a repeat with another content is refused
    timer_id         text,
    state            text        NOT NULL CHECK (state IN ('active', 'done', 'cancelled')),
    last_fired_at    timestamptz,
    created_at       timestamptz NOT NULL,
    ended_at         timestamptz,
    PRIMARY KEY (app_id, user_id, schedule_id),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user,
    CHECK ((kind = 'once') = (fire_at IS NOT NULL)),
    CHECK ((kind = 'cron') = (cron IS NOT NULL))
);
CREATE INDEX schedule_active ON schedule (app_id, user_id, merge_key) WHERE state = 'active';

-- A conversation's user can stop the reply in progress. Messages up to this position are not answered
-- any more: a turn that works on them stops, and one that has not started leaves them out.
ALTER TABLE thread ADD COLUMN stopped_through bigint NOT NULL DEFAULT 0;
