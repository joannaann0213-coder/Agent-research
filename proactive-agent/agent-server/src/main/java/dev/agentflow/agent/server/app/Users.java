package dev.agentflow.agent.server.app;

import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.time.AgentClock;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** The end users of applications. A user exists from the first time its application mentions it. */
@Service
public class Users {

    public record User(String appId, String userId, int tokenEpoch) {
    }

    private final JdbcClient jdbc;
    private final AgentClock clock;

    public Users(JdbcClient jdbc, AgentClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** The user, created if this is the first mention. The id is checked here, once, for everything that follows. */
    public User ensure(String appId, String userId) {
        Names.userId(userId);
        return jdbc.sql("""
                INSERT INTO app_user (app_id, user_id, created_at) VALUES (:appId, :userId, :now)
                ON CONFLICT (app_id, user_id) DO UPDATE SET user_id = excluded.user_id
                RETURNING token_epoch
                """)
                .param("appId", appId).param("userId", userId).param("now", Db.ts(clock.now()))
                .query((rs, row) -> new User(appId, userId, rs.getInt("token_epoch")))
                .single();
    }

    public Optional<User> find(String appId, String userId) {
        return jdbc.sql("SELECT token_epoch FROM app_user WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", userId)
                .query((rs, row) -> new User(appId, userId, rs.getInt("token_epoch")))
                .optional();
    }

    /**
     * Takes the lock of the user's row and returns the next position in the user's stream. The lock
     * is held until the transaction ends, so positions are committed in the order they are handed
     * out: a reader that has seen position N will never see a lower one appear later.
     */
    public long nextSeq(String appId, String userId) {
        return jdbc.sql("""
                UPDATE app_user SET next_seq = next_seq + 1 WHERE app_id = :appId AND user_id = :userId
                RETURNING next_seq - 1
                """)
                .param("appId", appId).param("userId", userId)
                .query(Long.class).single();
    }

    /** Takes the lock of the user's row, so that everything about this user happens one at a time until the transaction ends. */
    public void lock(String appId, String userId) {
        jdbc.sql("SELECT 1 FROM app_user WHERE app_id = :appId AND user_id = :userId FOR UPDATE")
                .param("appId", appId).param("userId", userId)
                .query(Integer.class).single();
    }

    /** Invalidates every token of the user that is out. A user nobody mentioned yet has none. */
    public void raiseTokenEpoch(String appId, String userId) {
        Names.userId(userId);
        jdbc.sql("UPDATE app_user SET token_epoch = token_epoch + 1 WHERE app_id = :appId AND user_id = :userId")
                .param("appId", appId).param("userId", userId)
                .update();
    }
}
