-- Where the credentials of MCP servers are kept: in vaults on the agent platform, one for each
-- application and one for each user who has credentials of their own. The secrets go to the platform
-- and nowhere else: this service keeps only what is needed to find and replace them.
CREATE TABLE vault (
    app_id        text        COLLATE "C" NOT NULL REFERENCES app,
    user_id       text        COLLATE "C" NOT NULL,   -- '' for the application's own vault
    cma_vault_id  text        NOT NULL,
    created_at    timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id)
);

CREATE TABLE mcp_credential (
    app_id             text        COLLATE "C" NOT NULL REFERENCES app,
    user_id            text        COLLATE "C" NOT NULL,   -- '' for the application's
    name               text        COLLATE "C" NOT NULL,
    mcp_server_url     text        NOT NULL,
    kind               text        NOT NULL CHECK (kind IN ('bearer', 'oauth')),
    cma_credential_id  text        NOT NULL,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    PRIMARY KEY (app_id, user_id, name)
);

-- The vaults a line's session was created with. They cannot be changed on a session; when they
-- should be others, the line's next turn starts a new session.
ALTER TABLE thread ADD COLUMN cma_vault_ids text NOT NULL DEFAULT '';
