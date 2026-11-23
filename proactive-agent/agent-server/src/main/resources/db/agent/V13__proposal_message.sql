-- A task that waits for the user's confirmation shows in the conversation as a message of its own,
-- which names the task: an application that shows nothing but the conversation shows it too, and the
-- user can answer it there.
ALTER TABLE message ADD COLUMN task_id text COLLATE "C";
ALTER TABLE message DROP CONSTRAINT message_kind_check;
ALTER TABLE message ADD CONSTRAINT message_kind_check
    CHECK (kind IN ('message', 'reply', 'notification', 'question', 'proposal'));
