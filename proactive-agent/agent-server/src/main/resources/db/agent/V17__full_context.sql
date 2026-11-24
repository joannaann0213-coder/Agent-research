-- A main agent stands by for the user, so it has to know how every task of its conversation
-- stands, not only what came of it. Two more things are told to it: that a worker waits for a
-- person's decision, and that the decision came.
ALTER TABLE task_note DROP CONSTRAINT task_note_kind_check;
ALTER TABLE task_note ADD CONSTRAINT task_note_kind_check
    CHECK (kind IN ('assignment', 'message', 'event', 'schedule',
                    'report', 'failure', 'closed', 'assigned', 'waiting', 'decided'));

-- Whatever a task has for the main agent is given to the next turn of the main agent, whichever
-- that is: the one that the note itself wakes, or an earlier one, such as the user's next message.
-- A note about something that is held back until a later time is not given before that time.
ALTER TABLE task_note ADD COLUMN ready_at timestamptz;
CREATE INDEX task_note_unread ON task_note (app_id, user_id, created_at) WHERE direction = 'to_main' AND read_at IS NULL;
