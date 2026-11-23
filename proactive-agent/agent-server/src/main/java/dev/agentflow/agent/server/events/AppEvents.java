package dev.agentflow.agent.server.events;

import dev.agentflow.agent.api.EventAccepted;
import dev.agentflow.agent.api.PostEventRequest;
import dev.agentflow.agent.server.actions.Actions;
import dev.agentflow.agent.server.api.AgentException;
import dev.agentflow.agent.server.app.Users;
import dev.agentflow.agent.server.chat.Lines;
import dev.agentflow.agent.server.chat.Messages;
import dev.agentflow.agent.server.chat.Threads;
import dev.agentflow.agent.server.core.Db;
import dev.agentflow.agent.server.core.Ids;
import dev.agentflow.agent.server.core.Json;
import dev.agentflow.agent.server.core.Names;
import dev.agentflow.agent.server.relay.InboxRelay;
import dev.agentflow.agent.server.time.AgentClock;
import dev.agentflow.inbox.api.CanonicalJson;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.Hashing;
import dev.agentflow.inbox.api.InboxJson;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.WriteEventRequest;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * What applications tell the agent about their users from their own systems. An event is stored
 * here and announced to the Event Inbox in the same transaction; the Inbox turns it into a run, in
 * which the agent reads the event and decides whether the user needs to hear about it. The run is
 * one of the subject's line, or, when the agent is a main agent, of the conversation it serves.
 */
@Service
public class AppEvents {

    /** The type of the Inbox event that announces an application's event. Its payload names the event; the data stays here. */
    public static final String EVENT_TYPE = "agent.app_event.v1";

    /** Types that begin like this are the service's own. */
    public static final String RESERVED_PREFIX = "agent.";

    public static final int MAX_DATA_BYTES = 16 * 1024;
    public static final Duration MAX_DELAY = Duration.ofDays(366);

    private static final Pattern TYPE = Pattern.compile("[a-z0-9][a-z0-9_.-]{0,63}");

    /** An application's event as it is stored. */
    public record AppEvent(String eventId, String type, String subject, String conversationId, JsonNode data,
            Instant createdAt, Instant deliverAt) {
    }

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;
    private final Users users;
    private final Lines lines;
    private final InboxRelay relay;
    private final Actions actions;
    private final AgentClock clock;
    private final Ids ids;

    public AppEvents(JdbcClient jdbc, TransactionTemplate tx, Users users, Lines lines, InboxRelay relay, Actions actions,
            AgentClock clock, Ids ids) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.users = users;
        this.lines = lines;
        this.relay = relay;
        this.actions = actions;
        this.clock = clock;
        this.ids = ids;
    }

    /** Stores an event and queues the Inbox event that will get it handled. */
    public EventAccepted post(String appId, String userId, PostEventRequest request) {
        if (request.type() == null || !TYPE.matcher(request.type()).matches()) {
            throw AgentException.invalid("type is required: 1 to 64 characters of a-z, 0-9, _ . and -, starting with a "
                    + "letter or a digit, for example order.shipped");
        }
        if (request.type().startsWith(RESERVED_PREFIX)) {
            throw AgentException.invalid("types that start with " + RESERVED_PREFIX + " are the service's own");
        }
        String type = request.type();
        String subject = request.subject() == null ? type : Names.localId(request.subject(), "subject");
        String eventId = request.eventId() == null ? ids.next("evt") : Names.localId(request.eventId(), "event_id");
        String conversationId = request.conversationId() == null ? Messages.DEFAULT_CONVERSATION
                : Names.localId(request.conversationId(), "conversation_id");
        String agentKey = request.agent() == null ? null : Names.agentKey(request.agent());
        JsonNode data = request.data() == null || request.data().isNull() ? Json.STORED.createObjectNode() : request.data();
        if (!data.isObject()) {
            throw AgentException.invalid("data must be a JSON object");
        }
        String dataJson = Json.STORED.writeValueAsString(data);
        if (dataJson.getBytes(StandardCharsets.UTF_8).length > MAX_DATA_BYTES) {
            throw AgentException.tooLarge("data has at most " + MAX_DATA_BYTES + " bytes; pass a reference to large content, "
                    + "not the content");
        }
        Instant now = clock.now();
        if (request.deliverAt() != null && request.deliverAt().isAfter(now.plus(MAX_DELAY))) {
            throw AgentException.invalid("deliver_at is at most " + MAX_DELAY.toDays() + " days ahead");
        }
        Instant deliverAt = request.deliverAt() != null && request.deliverAt().isAfter(now) ? request.deliverAt() : null;

        ObjectNode said = Json.STORED.createObjectNode();
        said.put("type", type).put("subject", subject).put("conversation_id", conversationId);
        said.put("agent", agentKey);
        said.put("deliver_at", request.deliverAt() == null ? null : request.deliverAt().toString());
        said.set("data", data);
        String fingerprint = Hashing.sha256Hex(CanonicalJson.of(said).getBytes(StandardCharsets.UTF_8));

        return tx.execute(status -> {
            users.ensure(appId, userId);
            users.lock(appId, userId);

            Optional<Stored> earlier = stored(appId, userId, eventId);
            if (earlier.isPresent()) {
                if (!earlier.get().fingerprint().equals(fingerprint)) {
                    throw AgentException.conflict("event_id " + eventId + " was already used for another event of this user");
                }
                AppEvent event = earlier.get().event();
                return new EventAccepted(eventId, event.type(), event.subject(), event.conversationId(), event.createdAt(),
                        event.deliverAt(), true);
            }

            // The subject's own line, or, for a main agent, the line of the conversation it serves.
            Threads.Thread line = lines.background(appId, userId, subject, agentKey, conversationId,
                    "; a subject keeps the agent of its first event");
            String mergeKey = line.mergeKey();

            jdbc.sql("""
                    INSERT INTO app_event (app_id, user_id, event_id, type, subject, conversation_id, data, fingerprint,
                                           deliver_at, created_at)
                    VALUES (:appId, :userId, :eventId, :type, :subject, :conversationId, CAST(:data AS jsonb), :fingerprint,
                            :deliverAt, :now)
                    """)
                    .param("appId", appId).param("userId", userId).param("eventId", eventId).param("type", type)
                    .param("subject", subject).param("conversationId", conversationId).param("data", dataJson)
                    .param("fingerprint", fingerprint).param("deliverAt", Db.ts(deliverAt)).param("now", Db.ts(now))
                    .update();

            // Something new happened to the subject: whatever its line waits for people to decide no longer applies.
            // An event that is held back counts from when it is due, so it does not withdraw anything now.
            if (deliverAt == null) {
                actions.supersede(appId, userId, mergeKey, line.chat()
                        ? "something new came in for you before this was decided; it comes in the next turn"
                        : Actions.supersededNote(mergeKey));
            }

            ObjectNode payload = InboxJson.mapper().createObjectNode();
            payload.put("event_id", eventId);
            relay.enqueue(appId, userId, WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, EVENT_TYPE)
                    .sourceEventId("evt:" + eventId)
                    .mergeKey(mergeKey)
                    // Not the lane of conversations: work in the background never makes a user wait for a reply.
                    .lane(Lane.BACKGROUND)
                    .interrupt(false)
                    .occurredAt(now)
                    .readyAt(deliverAt)
                    .payload(payload)
                    .build());
            return new EventAccepted(eventId, type, subject, conversationId, now, deliverAt, false);
        });
    }

    /**
     * Something that this service itself has to tell the agent of a line, as an event like the
     * application's: it wakes the agent in the background, which reads it with read_events. Stored
     * once per {@code eventId}. In the caller's transaction, under the user's lock.
     *
     * @param type a type of the service's own, which begins with {@link #RESERVED_PREFIX}
     */
    public void fromService(String appId, String userId, String mergeKey, String conversationId, String eventId, String type,
            String subject, ObjectNode data) {
        if (stored(appId, userId, eventId).isPresent()) {
            return;
        }
        Instant now = clock.now();
        String dataJson = Json.STORED.writeValueAsString(data);
        jdbc.sql("""
                INSERT INTO app_event (app_id, user_id, event_id, type, subject, conversation_id, data, fingerprint, created_at)
                VALUES (:appId, :userId, :eventId, :type, :subject, :conversationId, CAST(:data AS jsonb), :fingerprint, :now)
                """)
                .param("appId", appId).param("userId", userId).param("eventId", eventId).param("type", type)
                .param("subject", subject).param("conversationId", conversationId).param("data", dataJson)
                .param("fingerprint", Hashing.sha256Hex(dataJson.getBytes(StandardCharsets.UTF_8))).param("now", Db.ts(now))
                .update();
        ObjectNode payload = InboxJson.mapper().createObjectNode();
        payload.put("event_id", eventId);
        relay.enqueue(appId, userId, WriteEventRequest.builder(EventSource.AGENT_SELF, EVENT_TYPE)
                .sourceEventId("evt:" + eventId)
                .mergeKey(mergeKey)
                .lane(Lane.BACKGROUND)
                .interrupt(false)
                .occurredAt(now)
                .payload(payload)
                .build());
    }

    public Optional<AppEvent> find(String appId, String userId, String eventId) {
        return stored(appId, userId, eventId).map(Stored::event);
    }

    private record Stored(AppEvent event, String fingerprint) {
    }

    private Optional<Stored> stored(String appId, String userId, String eventId) {
        return jdbc.sql("""
                SELECT event_id, type, subject, conversation_id, data::text AS data, fingerprint, deliver_at, created_at
                  FROM app_event WHERE app_id = :appId AND user_id = :userId AND event_id = :eventId
                """)
                .param("appId", appId).param("userId", userId).param("eventId", eventId)
                .query(AppEvents::row)
                .optional();
    }

    private static Stored row(ResultSet rs, int rowNumber) throws SQLException {
        return new Stored(new AppEvent(rs.getString("event_id"), rs.getString("type"), rs.getString("subject"),
                rs.getString("conversation_id"), Json.STORED.readTree(rs.getString("data")), Db.instant(rs, "created_at"),
                Db.instant(rs, "deliver_at")), rs.getString("fingerprint"));
    }
}
