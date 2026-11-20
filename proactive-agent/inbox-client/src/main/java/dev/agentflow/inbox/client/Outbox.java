package dev.agentflow.inbox.client;

import java.util.List;

/**
 * Stand-in for the Outbox of the architecture, which decides what is sent, where and at what pace.
 * Only the one property the Event Inbox relies on is modelled here: delivery is deduplicated on
 * {@link OutboxMessage#dedupeKey()}, so at-least-once execution does not become at-least-once messaging.
 */
public interface Outbox {

    /** @return true when the message was delivered, false when its key had been delivered before */
    boolean send(OutboxMessage message);

    /** Everything delivered so far, in no particular order. */
    List<OutboxMessage> delivered();
}
