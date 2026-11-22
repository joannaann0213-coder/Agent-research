package dev.agentflow.agent.server.auth;

import dev.agentflow.agent.server.api.AgentException;

/**
 * Who makes a request.
 *
 * @param appId the application; {@code null} for an administrator
 * @param userId the end user, when the request carries a user token; otherwise {@code null}
 * @param keyId the API key that authenticated an application; otherwise {@code null}
 */
public record Caller(Kind kind, String appId, String userId, String keyId) {

    public static final String REQUEST_ATTRIBUTE = Caller.class.getName();

    public enum Kind {
        /** An operator of the service, with an administration key. */
        ADMIN,
        /** An application's back end, with an API key. */
        APP,
        /** An end user's client, with a token the application's back end got for that user. */
        USER
    }

    /** Who did it, for records: {@code app:acme} or {@code user:acme/u42}. */
    public String actor() {
        return switch (kind) {
            case ADMIN -> "admin";
            case APP -> "app:" + appId;
            case USER -> "user:" + appId + "/" + userId;
        };
    }

    /** The application of a call that only an application's back end may make. */
    public String requireApp() {
        if (kind != Kind.APP) {
            throw AgentException.forbidden("this call needs the application's API key");
        }
        return appId;
    }

    /**
     * The application of a call about {@code userId} that the application's back end or that user's
     * own client may make.
     */
    public String requireAppOrUser(String userId) {
        if (kind == Kind.APP || (kind == Kind.USER && this.userId.equals(userId))) {
            return appId;
        }
        throw AgentException.forbidden("a user token is only good for its own user");
    }
}
