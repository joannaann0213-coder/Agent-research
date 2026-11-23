-- A main agent asks the user in passing: its turn does not wait for the answer, which comes as the
-- user's next message and a turn of its own. Everything that concerns the user comes in on the main
-- agent's line, so a question that waited as other questions do would be withdrawn by the next event
-- or report that has nothing to do with it.
ALTER TABLE action
    ADD COLUMN in_passing boolean NOT NULL DEFAULT false,
    -- Asked in a turn that the user did not begin: it counts against what the user is sent in a day.
    ADD COLUMN unasked    boolean NOT NULL DEFAULT false;

-- No turn waits for such a question, so nothing wakes when its time is up: the sweep finds it.
CREATE INDEX action_in_passing_due ON action (expires_at) WHERE status = 'pending' AND in_passing;
CREATE INDEX action_unasked ON action (app_id, user_id, created_at) WHERE unasked;
