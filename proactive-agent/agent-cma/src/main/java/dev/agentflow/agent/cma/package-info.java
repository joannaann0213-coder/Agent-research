/**
 * The port to Claude Managed Agents.
 *
 * <p>Claude Managed Agents hosts the agent loop and the container its tools run in. The agent
 * service keeps what happens around that: which events reach an agent and when, who may approve a
 * tool call, what the user gets to see. This package is the line between the two. It names the few
 * things the service asks of the platform ({@link dev.agentflow.agent.cma.ManagedAgents}) in the
 * service's own terms, so that nothing above this package depends on the SDK or on the wire format.
 *
 * <p>{@code sdk} holds the adapter over the official Java SDK, {@code fake} an in-process stand-in
 * with a scripted model.
 */
package dev.agentflow.agent.cma;
