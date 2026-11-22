-- An agent of an application: what the application configured, and what was set up on the agent
-- platform for it.
CREATE TABLE agent_profile (
    app_id              text        COLLATE "C" NOT NULL REFERENCES app,
    agent_key           text        COLLATE "C" NOT NULL,
    config              jsonb       NOT NULL,   -- the configuration with every default filled in
    revision            integer     NOT NULL,   -- counts the application's changes, from 1
    -- The platform's side. The agent there is versioned; a conversation is pinned to the version
    -- it started on, so a change never alters a turn that is under way.
    cma_environment_id  text,
    cma_agent_id        text,
    cma_agent_version   integer,
    -- A hash of what was last sent to the platform. It differs from the hash of what this service
    -- would send now after a change of the configuration, and also after an upgrade of the service
    -- that gives agents a new tool; either way the platform is brought up to date before the next use.
    provisioned_hash    text,
    created_at          timestamptz NOT NULL,
    updated_at          timestamptz NOT NULL,
    PRIMARY KEY (app_id, agent_key)
);
