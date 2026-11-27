-- How much an agent does unasked is a setting, and a setting is tuned by numbers: how often the
-- agent was woken and for what, what it then did, how long a user waited, and what it all used.
-- A turn keeps what it was and what its requests to the model used.
ALTER TABLE turn
    -- chat: a user wrote. background: something happened, fell due, or a worker reported. worker: a
    -- worker's turn on a task.
    ADD COLUMN kind                  text CHECK (kind IN ('chat', 'background', 'worker')),
    ADD COLUMN agent_key             text COLLATE "C",
    ADD COLUMN model_requests        integer NOT NULL DEFAULT 0,
    -- Input that was neither read from the cache nor written to it; the two below say what was.
    ADD COLUMN input_tokens          bigint  NOT NULL DEFAULT 0,
    ADD COLUMN output_tokens         bigint  NOT NULL DEFAULT 0,
    ADD COLUMN cache_read_tokens     bigint  NOT NULL DEFAULT 0,
    ADD COLUMN cache_creation_tokens bigint  NOT NULL DEFAULT 0,
    -- For a reply: how long the user waited for it, from the oldest of the messages that it answers.
    ADD COLUMN waited_ms             bigint;

UPDATE turn SET kind = CASE work ->> 'kind' WHEN 'chat' THEN 'chat' WHEN 'job' THEN 'worker' WHEN 'task' THEN 'background' END
 WHERE work IS NOT NULL;
UPDATE turn t SET agent_key = line.agent_key
  FROM thread line
 WHERE line.app_id = t.app_id AND line.user_id = t.user_id AND line.merge_key = t.merge_key;

CREATE INDEX turn_by_app_time  ON turn (app_id, created_at);
CREATE INDEX turn_by_user_time ON turn (app_id, user_id, created_at);
CREATE INDEX message_by_app_time ON message (app_id, created_at);
CREATE INDEX delegated_task_by_app_time ON delegated_task (app_id, created_at);
