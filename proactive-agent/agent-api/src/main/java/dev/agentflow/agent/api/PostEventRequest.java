package dev.agentflow.agent.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Something that happened in the application's own systems and that the agent should look at for a
 * user: an order shipped, a ticket was answered, an alert fired.
 *
 * @param eventId the caller's id for the event, unique for the user. Sending the same event again
 *     is harmless; the service makes an id up when it is absent
 * @param type what happened, in the application's own words, for example {@code order.shipped}
 *     (required)
 * @param subject what it happened to, for example {@code order-1234}. Events with the same subject
 *     are handled one after the other by an agent that remembers the earlier ones; the type when
 *     absent
 * @param agent the key of the agent that handles the subject; {@code default} when absent. It is
 *     fixed by the subject's first event
 * @param conversationId the conversation in which the user is told, if the agent decides to tell
 *     them; {@code default} when absent
 * @param data the details, as a JSON object. The agent reads them as information from another
 *     system, not as instructions
 * @param deliverAt do not handle the event before this time; at once when absent or past
 */
public record PostEventRequest(String eventId, String type, String subject, String agent, String conversationId,
        JsonNode data, Instant deliverAt) {
}
