-- A schedule that the application sets for another agent, in a conversation that a main agent
-- serves, falls due on the main agent's line, like everything that concerns the user, and is handed
-- to that agent there as a task of the main agent. The subject that the task is kept under can then
-- no longer be read off the line's key, so the schedule keeps it.
ALTER TABLE schedule ADD COLUMN subject text COLLATE "C";

-- What the application scheduled for a worker, as it is handed to the worker when it is due.
ALTER TABLE task_note DROP CONSTRAINT task_note_kind_check;
ALTER TABLE task_note ADD CONSTRAINT task_note_kind_check
    CHECK (kind IN ('assignment', 'message', 'event', 'schedule', 'report', 'failure', 'closed', 'assigned'));
ALTER TABLE task_note DROP CONSTRAINT task_note_side_check;
ALTER TABLE task_note ADD CONSTRAINT task_note_side_check
    CHECK ((direction = 'to_worker') = (kind IN ('assignment', 'message', 'event', 'schedule')));
