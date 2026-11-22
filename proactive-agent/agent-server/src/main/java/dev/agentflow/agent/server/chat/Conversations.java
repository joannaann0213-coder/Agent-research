package dev.agentflow.agent.server.chat;

import dev.agentflow.agent.api.StopResult;
import dev.agentflow.agent.server.actions.Actions;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.core.Names;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stopping a conversation's reply. The user's messages so far are not answered any more: a turn
 * that works on them stops its session, and one that has not started leaves them out. What the
 * conversation waits for people to decide is withdrawn. A message sent after the stop is answered as
 * usual, and nothing else of the user is touched: only this conversation's line stops.
 */
@Service
public class Conversations {

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final Threads threads;
    private final Actions actions;

    public Conversations(JdbcClient jdbc, TransactionTemplate tx, Users users, Threads threads, Actions actions) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.threads = threads;
        this.actions = actions;
    }

    public StopResult stop(String appId, String userId, String conversationId) {
        Names.userId(userId);
        Names.localId(conversationId, "conversation_id");
        String line = Threads.chatKey(conversationId);
        return tx.execute(status -> {
            if (users.find(appId, userId).isEmpty()) {
                return new StopResult(conversationId, 0, false);
            }
            users.lock(appId, userId);
            Optional<Threads.Thread> thread = threads.find(appId, userId, line);
            if (thread.isEmpty()) {
                return new StopResult(conversationId, 0, false);
            }
            long through = users.lastSeq(appId, userId);
            boolean inProgress = openTurns(appId, userId, line) > 0 || awaitsReply(appId, userId, conversationId, line,
                    thread.get().stoppedThrough());
            threads.stopThrough(thread.get(), through);
            actions.supersede(appId, userId, line, "the user stopped the reply");
            return new StopResult(conversationId, Math.max(through, thread.get().stoppedThrough()), inProgress);
        });
    }

    private long openTurns(String appId, String userId, String line) {
        return jdbc.sql("SELECT count(*) FROM turn WHERE app_id = :appId AND user_id = :userId AND merge_key = :line AND state = 'open'")
                .param("appId", appId).param("userId", userId).param("line", line)
                .query(Long.class).single();
    }

    /** Whether the conversation's last word on its line is the user's, not stopped yet: a reply to it is to come. */
    private boolean awaitsReply(String appId, String userId, String conversationId, String line, long stoppedThrough) {
        return jdbc.sql("""
                SELECT role = 'user' AND seq > :stoppedThrough FROM message
                 WHERE app_id = :appId AND user_id = :userId AND conversation_id = :conversationId AND merge_key = :line
                 ORDER BY seq DESC LIMIT 1
                """)
                .param("stoppedThrough", stoppedThrough).param("appId", appId).param("userId", userId)
                .param("conversationId", conversationId).param("line", line)
                .query(Boolean.class).optional().orElse(false);
    }
}
