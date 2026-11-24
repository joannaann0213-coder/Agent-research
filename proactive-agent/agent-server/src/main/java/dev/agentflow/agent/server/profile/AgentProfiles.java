package dev.agentflow.agent.server.profile;

import dev.agentflow.agent.api.AgentProfileRequest;
import dev.agentflow.agent.api.AgentProfileView;
import dev.agentflow.agent.api.CoordinatorConfig;
import dev.agentflow.agent.api.McpServerConfig;
import dev.agentflow.agent.api.Proactivity;
import dev.agentflow.agent.api.SandboxConfig;
import dev.agentflow.agent.api.WorkerConfig;
import dev.agentflow.agent.cma.AgentRef;
import dev.agentflow.agent.cma.AgentSpec;
import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.time.AgentClock;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The agents of applications. An application describes its agent in its own terms; this class
 * keeps the description and sets up what the agent platform needs for it: an agent there, which is
 * versioned, and an environment for the agent's containers. A main agent and its workers are each
 * an agent of their own on the platform; what joins them is kept here.
 *
 * <p>Saving a profile and setting up the platform happen in one transaction around the profile's
 * row. If the platform refuses or cannot be reached, nothing is saved and the caller is told; a
 * profile that is saved is therefore always one the platform has accepted.
 */
@Service
public class AgentProfiles {

    public static final String DEFAULT_MODEL = "claude-opus-5-5";

    /** A profile with what the platform has for it, ready to start a session on. */
    public record Ready(String appId, String agentKey, AgentProfileRequest config, int revision, AgentRef agent,
            String environmentId) {
    }

    /** The outcome of saving a profile. */
    public record Saved(AgentProfileView profile, boolean created) {
    }

    private static final Logger log = LoggerFactory.getLogger(AgentProfiles.class);

    private static final Pattern MODEL = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,99}");
    private static final Pattern SERVER_NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private static final Pattern HOST = Pattern.compile("(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}");
    private static final Set<String> EFFORTS = Set.of("low", "medium", "high", "xhigh", "max");
    private static final Set<String> NETWORKS = Set.of("none", "limited", "unrestricted");
    private static final Set<String> APPROVALS = Set.of("always_ask", "auto", "always_allow");
    private static final Set<String> APPROVERS = Set.of("app", "user");
    private static final int MAX_INSTRUCTIONS = 50_000;
    /** What a worker's role is named like. The same names are used where a user allows a role to start unasked. */
    public static final Pattern ROLE = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");
    private static final Set<String> STARTS = Set.of(WorkerConfig.ASK, WorkerConfig.ALWAYS_ASK, WorkerConfig.ALWAYS_ALLOW);
    private static final int MAX_WORKERS = 10;
    private static final int MAX_ROLE_DESCRIPTION = 500;
    private static final int MAX_ACTIVE_TASKS = 20;
    private static final int DEFAULT_ACTIVE_TASKS = 4;

    private record Row(String appId, String agentKey, AgentProfileRequest config, int revision, String environmentId,
            String agentId, Integer agentVersion, String provisionedHash, Instant createdAt, Instant updatedAt) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ManagedAgents cma;
    private final AgentClock clock;

    public AgentProfiles(JdbcClient jdbc, TransactionTemplate tx, ManagedAgents cma, AgentClock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.cma = cma;
        this.clock = clock;
    }

    /** Creates the profile or replaces its configuration, and brings the platform up to date. */
    public Saved save(String appId, String agentKey, AgentProfileRequest request) {
        Names.agentKey(agentKey);
        AgentProfileRequest config = normalise(request);
        String json = Json.STORED.writeValueAsString(config);
        return tx.execute(status -> {
            Instant now = clock.now();
            checkRoster(appId, agentKey, config.coordinator());
            boolean created = jdbc.sql("""
                    INSERT INTO agent_profile (app_id, agent_key, config, revision, created_at, updated_at)
                    VALUES (:appId, :agentKey, CAST(:config AS jsonb), 1, :now, :now)
                    ON CONFLICT (app_id, agent_key) DO NOTHING
                    """)
                    .param("appId", appId).param("agentKey", agentKey).param("config", json).param("now", Db.ts(now))
                    .update() == 1;
            Row row = lock(appId, agentKey).orElseThrow();
            if (!created && !row.config().equals(config)) {
                jdbc.sql("""
                        UPDATE agent_profile SET config = CAST(:config AS jsonb), revision = revision + 1, updated_at = :now
                         WHERE app_id = :appId AND agent_key = :agentKey
                        """)
                        .param("config", json).param("now", Db.ts(now)).param("appId", appId).param("agentKey", agentKey)
                        .update();
                row = lock(appId, agentKey).orElseThrow();
            }
            return new Saved(view(provision(row)), created);
        });
    }

    public AgentProfileView get(String appId, String agentKey) {
        return view(find(appId, agentKey, false)
                .orElseThrow(() -> AgentException.notFound("no agent " + agentKey + " in this application")));
    }

    /** The profile's configuration as it is stored, without bringing the platform up to date. */
    public AgentProfileRequest config(String appId, String agentKey) {
        return find(appId, agentKey, false).map(Row::config)
                .orElseThrow(() -> AgentException.notFound("no agent " + agentKey + " in this application"));
    }

    /** The profile's configuration as it is stored, if the application has such an agent. */
    public Optional<AgentProfileRequest> configIfThere(String appId, String agentKey) {
        return find(appId, agentKey, false).map(Row::config);
    }

    public boolean exists(String appId, String agentKey) {
        return jdbc.sql("SELECT count(*) FROM agent_profile WHERE app_id = :appId AND agent_key = :agentKey")
                .param("appId", appId).param("agentKey", agentKey)
                .query(Long.class).single() == 1;
    }

    public List<AgentProfileView> list(String appId) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId ORDER BY agent_key")
                .param("appId", appId)
                .query(AgentProfiles::row)
                .list().stream().map(AgentProfiles::view).toList();
    }

    /**
     * The profile, with the platform brought up to date first if it is not: after an upgrade of this
     * service, an agent may be due a tool or a note that its stored version does not have yet.
     */
    public Ready ready(String appId, String agentKey) {
        Row current = find(appId, agentKey, false)
                .orElseThrow(() -> AgentException.notFound("no agent " + agentKey + " in this application"));
        if (!ProfileSpec.of(appId, agentKey, current.config()).hash().equals(current.provisionedHash())) {
            current = tx.execute(status -> provision(lock(appId, agentKey).orElseThrow()));
        }
        return new Ready(appId, agentKey, current.config(), current.revision(),
                new AgentRef(current.agentId(), current.agentVersion()), current.environmentId());
    }

    /**
     * The configuration of the platform's agent {@code cmaAgentId}, from the profile that it was made
     * for. The platform of the demo mode asks for it when a restart of the service made it forget the agent.
     */
    public Optional<AgentSpec> platformAgent(String cmaAgentId) {
        return jdbc.sql(SELECT + " WHERE cma_agent_id = :agentId")
                .param("agentId", cmaAgentId)
                .query(AgentProfiles::row)
                .optional()
                .map(row -> ProfileSpec.of(row.appId(), row.agentKey(), row.config()).agent());
    }

    /**
     * A main agent delegates to agents that exist and are not main agents themselves, and an agent
     * that some main agent delegates to cannot become one: delegation is one level deep. This is what
     * keeps a worker from handing its task on, and a main agent's session from being a worker's.
     */
    private void checkRoster(String appId, String agentKey, CoordinatorConfig coordinator) {
        if (coordinator == null) {
            return;
        }
        for (WorkerConfig worker : coordinator.workers()) {
            if (worker.agent().equals(agentKey)) {
                throw AgentException.invalid("coordinator.workers: role " + worker.role() + " names this agent itself");
            }
            Optional<Row> found = find(appId, worker.agent(), false);
            if (found.isEmpty()) {
                throw AgentException.invalid("coordinator.workers: this application has no agent " + worker.agent()
                        + " for the role " + worker.role() + "; create it first with PUT /v1/agents/" + worker.agent());
            }
            if (found.get().config().coordinator() != null) {
                throw AgentException.invalid("coordinator.workers: agent " + worker.agent() + " is a main agent itself; "
                        + "a main agent delegates to ordinary agents only");
            }
        }
        Optional<String> delegatedToBy = jdbc.sql("""
                SELECT agent_key FROM agent_profile
                 WHERE app_id = :appId AND config -> 'coordinator' -> 'workers' @> CAST(:worker AS jsonb)
                 ORDER BY agent_key LIMIT 1
                """)
                .param("appId", appId).param("worker", "[{\"agent\": \"" + agentKey + "\"}]")
                .query(String.class).optional();
        if (delegatedToBy.isPresent()) {
            throw AgentException.invalid("coordinator: this agent is a worker of the main agent " + delegatedToBy.get()
                    + "; a worker cannot be a main agent");
        }
    }

    // ================================================================ the platform

    /** Sets up or updates the platform's agent and environment, unless they already match. Needs the row's lock. */
    private Row provision(Row row) {
        ProfileSpec spec = ProfileSpec.of(row.appId(), row.agentKey(), row.config());
        String hash = spec.hash();
        if (hash.equals(row.provisionedHash())) {
            return row;
        }
        String environmentId = row.environmentId();
        AgentRef agent;
        try {
            if (environmentId == null) {
                environmentId = cma.createEnvironment(spec.environment());
            } else {
                cma.updateEnvironment(environmentId, spec.environment());
            }
            agent = row.agentId() == null ? cma.createAgent(spec.agent()) : update(row, spec);
        } catch (CmaException e) {
            if (e.kind() == CmaException.Kind.INVALID_REQUEST) {
                throw AgentException.invalid("the agent platform refused this configuration: " + e.getMessage());
            }
            log.warn("the agent platform could not set up agent {} of app {}: {} {}", row.agentKey(), row.appId(), e.kind(),
                    e.getMessage());
            throw AgentException.unavailable("the agent platform is not available right now; nothing was changed, try again");
        }
        jdbc.sql("""
                UPDATE agent_profile
                   SET cma_environment_id = :environmentId, cma_agent_id = :agentId, cma_agent_version = :agentVersion,
                       provisioned_hash = :hash
                 WHERE app_id = :appId AND agent_key = :agentKey
                """)
                .param("environmentId", environmentId).param("agentId", agent.id()).param("agentVersion", agent.version())
                .param("hash", hash).param("appId", row.appId()).param("agentKey", row.agentKey())
                .update();
        return new Row(row.appId(), row.agentKey(), row.config(), row.revision(), environmentId, agent.id(),
                agent.version(), hash, row.createdAt(), row.updatedAt());
    }

    private AgentRef update(Row row, ProfileSpec spec) {
        AgentRef stored = new AgentRef(row.agentId(), row.agentVersion());
        try {
            return cma.updateAgent(stored, spec.agent());
        } catch (CmaException e) {
            if (e.kind() != CmaException.Kind.CONFLICT) {
                throw e;
            }
            // The agent moved on without this row knowing, for example when an earlier update went
            // through and its answer was lost. Start again from where the agent is.
            return cma.updateAgent(cma.getAgent(row.agentId()), spec.agent());
        }
    }

    // ================================================================ validation

    /** Checks the request and fills in every default, so that what is stored says everything. */
    private static AgentProfileRequest normalise(AgentProfileRequest request) {
        if (request.name() == null || request.name().isBlank() || request.name().length() > 200) {
            throw AgentException.invalid("name is required and has at most 200 characters");
        }
        String model = request.model() == null ? DEFAULT_MODEL : request.model();
        if (!MODEL.matcher(model).matches()) {
            throw AgentException.invalid("model is not a model id");
        }
        if (request.effort() != null && !EFFORTS.contains(request.effort())) {
            throw AgentException.invalid("effort must be one of low, medium, high, xhigh, max");
        }
        if (request.instructions() == null || request.instructions().isBlank()) {
            throw AgentException.invalid("instructions are required: they say what the agent is for");
        }
        if (request.instructions().length() > MAX_INSTRUCTIONS) {
            throw AgentException.invalid("instructions have at most " + MAX_INSTRUCTIONS + " characters");
        }
        java.math.BigDecimal budget = request.sessionBudgetUsd();
        if (budget != null && (budget.signum() <= 0 || budget.compareTo(new java.math.BigDecimal("10000")) > 0 || budget.scale() > 2)) {
            throw AgentException.invalid("session_budget_usd is an amount of dollars and cents, more than 0 and at most 10000");
        }
        String memory = request.memory() == null ? "none" : request.memory();
        if (!memory.equals("none") && !memory.equals("per_user")) {
            throw AgentException.invalid("memory is none or per_user");
        }
        SandboxConfig sandbox = sandbox(request.sandbox());
        if (memory.equals("per_user") && !Boolean.TRUE.equals(sandbox.enabled())) {
            throw AgentException.invalid("memory needs the sandbox: the agent keeps its notes in files there, with its file tools");
        }
        return new AgentProfileRequest(request.name(), model, request.effort(), request.instructions(),
                sandbox, servers(request.mcpServers()), budget == null ? null : budget.setScale(2), memory,
                coordinator(request.coordinator()));
    }

    /** The shape of what makes an agent a main agent. Whether the agents it names can be its workers is checked when it is saved. */
    private static CoordinatorConfig coordinator(CoordinatorConfig coordinator) {
        if (coordinator == null) {
            return null;
        }
        List<WorkerConfig> workers = coordinator.workers() == null ? List.of() : coordinator.workers();
        if (workers.size() > MAX_WORKERS) {
            throw AgentException.invalid("coordinator.workers takes at most " + MAX_WORKERS + " workers");
        }
        List<WorkerConfig> normalised = new ArrayList<>();
        Set<String> roles = new HashSet<>();
        for (WorkerConfig worker : workers) {
            if (worker == null || worker.role() == null || !ROLE.matcher(worker.role()).matches()) {
                throw AgentException.invalid("a worker's role must be 1 to 32 characters of a-z, 0-9, _ and -, starting with a letter or a digit");
            }
            if (!roles.add(worker.role())) {
                throw AgentException.invalid("worker roles are unique within a main agent: " + worker.role());
            }
            if (worker.agent() == null || !Names.AGENT_KEY.matcher(worker.agent()).matches()) {
                throw AgentException.invalid("the agent of worker role " + worker.role() + " must be the key of an agent of this application");
            }
            if (worker.description() == null || worker.description().isBlank()
                    || worker.description().length() > MAX_ROLE_DESCRIPTION) {
                throw AgentException.invalid("worker role " + worker.role() + " needs a description of at most "
                        + MAX_ROLE_DESCRIPTION + " characters: the main agent reads it to decide whom to give a task");
            }
            if (worker.start() != null && !STARTS.contains(worker.start())) {
                throw AgentException.invalid("the start of worker role " + worker.role() + " is one of ask, always_ask, always_allow");
            }
            normalised.add(new WorkerConfig(worker.role(), worker.agent(), worker.description().strip(), worker.startRule()));
        }
        Integer active = coordinator.maxActiveTasks();
        if (active != null && (active < 1 || active > MAX_ACTIVE_TASKS)) {
            throw AgentException.invalid("coordinator.max_active_tasks is between 1 and " + MAX_ACTIVE_TASKS);
        }
        if (coordinator.proactivity() != null && !Proactivity.LEVELS.contains(coordinator.proactivity())) {
            throw AgentException.invalid("coordinator.proactivity is one of " + String.join(", ", Proactivity.LEVELS));
        }
        return new CoordinatorConfig(List.copyOf(normalised), active == null ? DEFAULT_ACTIVE_TASKS : active,
                coordinator.proactivity());
    }

    private static SandboxConfig sandbox(SandboxConfig sandbox) {
        if (sandbox == null) {
            return new SandboxConfig(false, false, false, "none", List.of());
        }
        boolean enabled = Boolean.TRUE.equals(sandbox.enabled());
        boolean webSearch = Boolean.TRUE.equals(sandbox.webSearch());
        boolean webFetch = Boolean.TRUE.equals(sandbox.webFetch());
        String network = sandbox.network() == null ? "none" : sandbox.network();
        List<String> hosts = sandbox.allowedHosts() == null ? List.of() : List.copyOf(sandbox.allowedHosts());
        if (!NETWORKS.contains(network)) {
            throw AgentException.invalid("sandbox.network must be one of none, limited, unrestricted");
        }
        if (!enabled && (webSearch || webFetch || !network.equals("none") || !hosts.isEmpty())) {
            throw AgentException.invalid("sandbox.enabled must be true for any other sandbox setting to apply");
        }
        if ((webSearch || webFetch) && network.equals("none")) {
            throw AgentException.invalid("the web tools need sandbox.network to be limited or unrestricted");
        }
        if (network.equals("limited") == hosts.isEmpty()) {
            throw AgentException.invalid("sandbox.allowed_hosts lists the hosts of a limited network, and only of that");
        }
        if (hosts.size() > 50 || new HashSet<>(hosts).size() != hosts.size()) {
            throw AgentException.invalid("sandbox.allowed_hosts takes at most 50 hosts, each once");
        }
        for (String host : hosts) {
            if (host == null || !HOST.matcher(host).matches()) {
                throw AgentException.invalid("sandbox.allowed_hosts takes plain host names such as api.example.com: " + host);
            }
        }
        return new SandboxConfig(enabled, webSearch, webFetch, network, hosts);
    }

    private static List<McpServerConfig> servers(List<McpServerConfig> servers) {
        if (servers == null) {
            return List.of();
        }
        if (servers.size() > 20) {
            throw AgentException.invalid("mcp_servers takes at most 20 servers");
        }
        List<McpServerConfig> normalised = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (McpServerConfig server : servers) {
            if (server == null || server.name() == null || !SERVER_NAME.matcher(server.name()).matches()) {
                throw AgentException.invalid("an MCP server's name must be 1 to 64 characters of a-z, 0-9, _ and -, starting with a letter or a digit");
            }
            if (!names.add(server.name())) {
                throw AgentException.invalid("MCP server names are unique within an agent: " + server.name());
            }
            if (!httpsUrl(server.url())) {
                throw AgentException.invalid("the url of MCP server " + server.name() + " must be an https URL");
            }
            String approval = server.approval() == null ? "always_ask" : server.approval();
            String approver = server.approver() == null ? "app" : server.approver();
            if (!APPROVALS.contains(approval)) {
                throw AgentException.invalid("approval must be one of always_ask, auto, always_allow");
            }
            if (!APPROVERS.contains(approver)) {
                throw AgentException.invalid("approver must be app or user");
            }
            normalised.add(new McpServerConfig(server.name(), server.url(), approval, approver));
        }
        return List.copyOf(normalised);
    }

    private static boolean httpsUrl(String url) {
        if (url == null || url.length() > 2000) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // ================================================================ rows

    private static final String SELECT = """
            SELECT app_id, agent_key, config::text AS config, revision, cma_environment_id, cma_agent_id,
                   cma_agent_version, provisioned_hash, created_at, updated_at
              FROM agent_profile
            """;

    private Optional<Row> lock(String appId, String agentKey) {
        return find(appId, agentKey, true);
    }

    private Optional<Row> find(String appId, String agentKey, boolean forUpdate) {
        return jdbc.sql(SELECT + " WHERE app_id = :appId AND agent_key = :agentKey" + (forUpdate ? " FOR UPDATE" : ""))
                .param("appId", appId).param("agentKey", agentKey)
                .query(AgentProfiles::row)
                .optional();
    }

    private static Row row(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        int version = rs.getInt("cma_agent_version");
        return new Row(rs.getString("app_id"), rs.getString("agent_key"),
                Json.STORED.readValue(rs.getString("config"), AgentProfileRequest.class), rs.getInt("revision"),
                rs.getString("cma_environment_id"), rs.getString("cma_agent_id"), rs.wasNull() ? null : version,
                rs.getString("provisioned_hash"), Db.instant(rs, "created_at"), Db.instant(rs, "updated_at"));
    }

    private static AgentProfileView view(Row row) {
        AgentProfileRequest config = row.config();
        return new AgentProfileView(row.agentKey(), config.name(), config.model(), config.effort(), config.instructions(),
                config.sandbox(), config.mcpServers(), config.sessionBudgetUsd(), config.memory() == null ? "none" : config.memory(),
                row.revision(), row.createdAt(), row.updatedAt(), config.coordinator());
    }
}
