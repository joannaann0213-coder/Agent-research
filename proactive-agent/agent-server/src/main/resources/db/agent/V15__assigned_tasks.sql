-- The application can hand work straight to a worker in a conversation that a main agent serves: an
-- event for another agent there becomes a task of the main agent instead of a line of its own that
-- the main agent knows nothing of. The worker starts at once, the main agent is told at once through
-- its own line, and the worker reports to the main agent like any other of its workers.
ALTER TABLE delegated_task
    -- agent: the main agent delegated it. app: the application handed it to the worker itself.
    ADD COLUMN created_by text NOT NULL DEFAULT 'agent' CHECK (created_by IN ('agent', 'app')),
    -- For a task of the application: the subject of its events. Later events of the subject go to the
    -- same task, and so to the session of the worker that remembers the earlier ones.
    ADD COLUMN subject    text COLLATE "C";
CREATE UNIQUE INDEX delegated_task_subject ON delegated_task (app_id, user_id, hub_key, subject)
    WHERE subject IS NOT NULL AND status <> 'closed';

-- event: what the application has for the worker. assigned: what tells the main agent of it.
ALTER TABLE task_note DROP CONSTRAINT task_note_kind_check;
ALTER TABLE task_note ADD CONSTRAINT task_note_kind_check
    CHECK (kind IN ('assignment', 'message', 'event', 'report', 'failure', 'closed', 'assigned'));
ALTER TABLE task_note DROP CONSTRAINT task_note_check;
ALTER TABLE task_note ADD CONSTRAINT task_note_side_check
    CHECK ((direction = 'to_worker') = (kind IN ('assignment', 'message', 'event')));
