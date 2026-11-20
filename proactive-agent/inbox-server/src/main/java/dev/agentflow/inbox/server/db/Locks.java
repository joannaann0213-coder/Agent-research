package dev.agentflow.inbox.server.db;

import java.sql.SQLException;

/** Recognises PostgreSQL lock failures. */
public final class Locks {

    private static final String LOCK_NOT_AVAILABLE = "55P03";
    private static final String DEADLOCK_DETECTED = "40P01";

    private Locks() {
    }

    /** True when a statement gave up waiting for a row lock ({@code lock_timeout}). */
    public static boolean isLockTimeout(Throwable error) {
        return hasState(error, LOCK_NOT_AVAILABLE);
    }

    public static boolean isDeadlock(Throwable error) {
        return hasState(error, DEADLOCK_DETECTED);
    }

    private static boolean hasState(Throwable error, String state) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && state.equals(sql.getSQLState())) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }
}
