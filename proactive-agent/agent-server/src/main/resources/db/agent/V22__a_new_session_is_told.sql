-- A line's session is replaced now and then: when its agent is configured anew, when credentials
-- change, when the session is lost, has used up its budget, or does not come to rest after an
-- interrupt. The new session knows nothing of the line's work so far. The service tells it what it
-- has on record, once, and notes here for which session of the line it has done so.
ALTER TABLE thread ADD COLUMN briefed_generation integer NOT NULL DEFAULT 0;

-- The sessions that are there have their work in them.
UPDATE thread SET briefed_generation = generation;

-- What a run told the user is looked up by the run: for a new session, and for the count of a turn's notifications.
CREATE INDEX message_run ON message (run_id) WHERE run_id IS NOT NULL;
