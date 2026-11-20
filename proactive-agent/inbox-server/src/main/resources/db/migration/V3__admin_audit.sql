-- Operator actions: replay (with or without force), manual dead-lettering, configuration changes.
CREATE TABLE admin_audit (
    audit_id        text        COLLATE "C" PRIMARY KEY,
    tenant_id       text,
    actor_client_id text        NOT NULL,
    operator        text,
    action          text        NOT NULL,
    target          jsonb       NOT NULL,
    params          jsonb,
    result          jsonb,
    at              timestamptz NOT NULL
);
CREATE INDEX admin_audit_at ON admin_audit (at DESC, audit_id DESC);

CREATE TRIGGER admin_audit_append_only
    BEFORE UPDATE OR DELETE ON admin_audit
    FOR EACH ROW EXECUTE FUNCTION inbox_reject_change('the audit log is append-only');
