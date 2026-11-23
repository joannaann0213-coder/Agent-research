-- A task that a main agent begins on its own initiative can wait for the user's confirmation before
-- it starts, and a user can allow a main agent to start the tasks of a role without asking each time.

-- proposed: the task waits for the user's confirmation; its worker has not been woken.
ALTER TABLE delegated_task DROP CONSTRAINT delegated_task_status_check;
ALTER TABLE delegated_task ADD CONSTRAINT delegated_task_status_check
    CHECK (status IN ('proposed', 'queued', 'working', 'idle', 'closed'));

ALTER TABLE delegated_task
    -- What the user is shown about the task, in the main agent's words: what it wants to do and why.
    ADD COLUMN summary       text,
    -- Why the task was allowed to start. conversation: the main agent delegated it, or started it, in
    -- a turn that the user began. user, app: the user's client or the application confirmed it. grant:
    -- the user's standing authorisation for the role. policy: the application lets the role start
    -- unasked. Empty while the task waits for a confirmation, and for one that was closed before it
    -- got one.
    ADD COLUMN authorized_by text CHECK (authorized_by IN ('conversation', 'user', 'app', 'grant', 'policy')),
    ADD COLUMN authorized_at timestamptz,
    -- For a task that waits for a confirmation: until when it can be given.
    ADD COLUMN expires_at    timestamptz;

-- Every task so far started when it was delegated: that was the rule of the service then.
UPDATE delegated_task SET authorized_by = 'policy', authorized_at = created_at;

ALTER TABLE delegated_task ADD CONSTRAINT delegated_task_authorized_check
    CHECK ((authorized_by IS NULL) = (authorized_at IS NULL)
       AND (status IN ('proposed', 'closed') OR authorized_by IS NOT NULL)
       AND (status <> 'proposed' OR (authorized_by IS NULL AND expires_at IS NOT NULL AND summary IS NOT NULL)));

-- A user's standing authorisation: the main agent may start the tasks of this role on its own
-- initiative without asking the user each time. The user gives it and takes it back; the application
-- can do both for them.
CREATE TABLE task_grant (
    app_id      text        COLLATE "C" NOT NULL,
    user_id     text        COLLATE "C" NOT NULL,
    agent_key   text        COLLATE "C" NOT NULL,   -- the main agent
    role        text        COLLATE "C" NOT NULL,   -- the role of its list of workers
    granted_by  text        NOT NULL CHECK (granted_by IN ('user', 'app')),
    created_at  timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, agent_key, role),
    FOREIGN KEY (app_id, user_id) REFERENCES app_user
);
