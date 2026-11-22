package dev.agentflow.agent.cma;

import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * The agent service's port to Claude Managed Agents: everything it asks of the platform that hosts
 * the agent loop, and nothing else.
 *
 * <p>Two implementations exist. {@code SdkManagedAgents} talks to the real service through the
 * official Java SDK. {@code FakeManagedAgents} is an in-process stand-in whose "model" is a script;
 * the tests run on it, and so does the local demo mode. One contract test suite runs against both.
 *
 * <p>Every method may throw {@link CmaException}. A method that changes something is not retried
 * inside the port: after a {@link CmaException#maybeDelivered() failure that may have been
 * delivered} the caller has to find out what happened, and only the caller can.
 */
public interface ManagedAgents {

    // ------------------------------------------------------------------ set-up, once per app or profile

    /** Creates the template for the containers that sessions run their tools in. Returns its id. */
    String createEnvironment(EnvironmentSpec spec);

    /** Replaces an environment's name, description and networking. Sessions that are running keep what they have. */
    void updateEnvironment(String environmentId, EnvironmentSpec spec);

    /** Creates an agent: model, instructions and tools. Returns its id and first version. */
    AgentRef createAgent(AgentSpec spec);

    /**
     * Replaces an agent's configuration. The agent must still be at {@code current}'s version
     * ({@link CmaException.Kind#CONFLICT} otherwise). Returns the reference to use from now on; the
     * version is unchanged when the configuration is.
     */
    AgentRef updateAgent(AgentRef current, AgentSpec spec);

    /** The agent's current version: what an update has to start from when the stored reference is out of date. */
    AgentRef getAgent(String agentId);

    // ------------------------------------------------------------------ credentials

    /** Creates a vault, the platform's store of credentials. Returns its id. */
    String createVault(String displayName, Map<String, String> metadata);

    /**
     * Stores a credential in a vault. A vault holds one active credential per MCP server
     * ({@link CmaException.Kind#CONFLICT} otherwise) and at most 20. Returns the credential's id.
     */
    String createCredential(String vaultId, McpCredential credential);

    /**
     * Replaces the bearer token of a stored credential. Sessions that have the vault use the new token
     * from their next call on. The server and the kind of a credential cannot change: archive it and
     * create another.
     */
    void updateCredential(String vaultId, String credentialId, String bearerToken);

    /** Archives a credential: its secrets are gone, and its server is free for another one. */
    void archiveCredential(String vaultId, String credentialId);

    /** The vault's active credentials. */
    List<McpCredential.Stored> listCredentials(String vaultId);

    // ------------------------------------------------------------------ sessions

    /**
     * Creates a session pinned to one version of an agent. It starts idle and does nothing until an
     * event is sent, so a session that is created and then forgotten costs nothing. Returns its id.
     */
    String createSession(SessionSpec spec);

    SessionInfo getSession(String sessionId);

    /**
     * The session's stored events, oldest first.
     *
     * @param createdSince only events created at or after this instant; {@code null} for all of them
     */
    List<CmaEvent> listEvents(String sessionId, Instant createdSince);

    /**
     * Opens the live stream of a session. It delivers only what happens after it is open: there is no
     * replay, so open it before sending, and read {@link #listEvents} to cover a gap (see
     * {@link EventTail}, which does both).
     */
    EventStream openStream(String sessionId);

    /**
     * Sends events to a session, in order. A message is queued behind the work in progress. While the
     * session waits for an answer to a tool call it accepts only answers and an interrupt; a message
     * is then refused with {@link CmaException.Kind#INVALID_REQUEST}.
     *
     * @return the events as the service stored them, with their ids
     */
    List<CmaEvent> send(String sessionId, List<Outgoing> events);
}
