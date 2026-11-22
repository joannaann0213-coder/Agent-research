package dev.agentflow.agent.server.memory;

import dev.agentflow.agent.cma.CmaException;
import dev.agentflow.agent.cma.ManagedAgents;
import dev.agentflow.agent.cma.SessionSpec;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.time.AgentClock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What an agent remembers about a user across sessions: a memory store on the agent platform, one per
 * user and agent. A conversation's session has it to read and write. A session in the background has
 * it to read only: there the agent works on what other systems say, and that must not end up in what
 * it remembers about the user.
 */
@Service
public class Memories {

    public static final String PER_USER = "per_user";

    private static final String WRITE = "What you remember about this user from earlier conversations. Keep here, in short notes, "
            + "what is worth knowing next time: their preferences, their situation, what you agreed with them. Never store secrets.";
    private static final String READ = "What you remember about this user from your conversations with them. Here you may read "
            + "it but not change it: in the background you work on what other systems say, which is not what the user told you.";

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final ManagedAgents cma;
    private final AgentClock clock;

    public Memories(JdbcClient jdbc, TransactionTemplate tx, ManagedAgents cma, AgentClock clock) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.cma = cma;
        this.clock = clock;
    }

    /** How a session of the line has the memory, for comparing sessions: {@code ""}, {@code rw} or {@code ro}. */
    public static String access(String memorySetting, boolean background) {
        return PER_USER.equals(memorySetting) ? (background ? "ro" : "rw") : "";
    }

    /** The mounts of a new session: the user's store, made the first time, or none. */
    public List<SessionSpec.MemoryMount> mounts(String appId, String userId, String agentKey, String access) {
        if (access.isEmpty()) {
            return List.of();
        }
        boolean readOnly = access.equals("ro");
        return List.of(new SessionSpec.MemoryMount(storeFor(appId, userId, agentKey), readOnly, readOnly ? READ : WRITE));
    }

    private String storeFor(String appId, String userId, String agentKey) {
        Optional<String> existing = find(appId, userId, agentKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        return tx.execute(status -> {
            jdbc.sql("SELECT pg_advisory_xact_lock(hashtext(:key))").param("key", "memory:" + appId + "/" + userId + "/" + agentKey)
                    .query(Object.class).single();
            Optional<String> made = find(appId, userId, agentKey);
            if (made.isPresent()) {
                return made.get();
            }
            String storeId;
            try {
                storeId = cma.createMemoryStore(appId + " / " + userId + " / " + agentKey,
                        "What agent " + agentKey + " of " + appId + " remembers about user " + userId,
                        Map.of("app", appId, "user", userId, "agent", agentKey));
            } catch (CmaException e) {
                throw e.kind() == CmaException.Kind.INVALID_REQUEST
                        ? AgentException.invalid("the agent platform refused the memory store: " + e.getMessage()) : e;
            }
            jdbc.sql("""
                    INSERT INTO memory_store (app_id, user_id, agent_key, cma_memory_store_id, created_at)
                    VALUES (:appId, :userId, :agentKey, :storeId, :now)
                    """)
                    .param("appId", appId).param("userId", userId).param("agentKey", agentKey).param("storeId", storeId)
                    .param("now", Db.ts(clock.now()))
                    .update();
            return storeId;
        });
    }

    private Optional<String> find(String appId, String userId, String agentKey) {
        return jdbc.sql("SELECT cma_memory_store_id FROM memory_store WHERE app_id = :appId AND user_id = :userId AND agent_key = :agentKey")
                .param("appId", appId).param("userId", userId).param("agentKey", agentKey)
                .query(String.class).optional();
    }
}
