-- What an agent remembers about a user across sessions: a memory store on the agent platform, one
-- per user and agent, created when the agent first needs it.
CREATE TABLE memory_store (
    app_id               text        COLLATE "C" NOT NULL REFERENCES app,
    user_id              text        COLLATE "C" NOT NULL,
    agent_key            text        COLLATE "C" NOT NULL,
    cma_memory_store_id  text        NOT NULL,
    created_at           timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, agent_key)
);

-- How a line's session has the user's memory: '' for not at all, 'rw' to read and write, 'ro' to read.
ALTER TABLE thread ADD COLUMN cma_memory text NOT NULL DEFAULT '';
