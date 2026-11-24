-- How much a user wants to hear from their main agent without having asked. The user's own setting;
-- where there is none, the default of the main agent applies, and where that has none, the service's.
ALTER TABLE app_user
    ADD COLUMN proactivity            text CHECK (proactivity IN ('proactive', 'balanced', 'quiet')),
    -- user, app, or agent when the user told the main agent in the conversation
    ADD COLUMN proactivity_set_by     text CHECK (proactivity_set_by IN ('user', 'app', 'agent')),
    ADD COLUMN proactivity_updated_at timestamptz;
