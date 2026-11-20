package dev.agentflow.inbox.server.timer;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.scheduling.support.CronExpression;

/**
 * A cron expression bound to a time zone. Occurrences are computed in the zone ("9 am" is the user's
 * 9 am, also across daylight-saving changes) and stored as UTC instants.
 */
public final class CronSchedule {

    private final CronExpression expression;
    private final ZoneId zone;

    private CronSchedule(CronExpression expression, ZoneId zone) {
        this.expression = expression;
        this.zone = zone;
    }

    /**
     * @param cron five fields (minute hour day-of-month month day-of-week) or six with a leading seconds field
     * @param zoneId IANA zone id; null means UTC
     * @throws IllegalArgumentException when the expression or the zone is not valid
     */
    public static CronSchedule parse(String cron, String zoneId) {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("cron expression is required");
        }
        String trimmed = cron.trim();
        String sixFields = trimmed.split("\\s+").length == 5 ? "0 " + trimmed : trimmed;
        ZoneId zone;
        try {
            zone = zoneId == null || zoneId.isBlank() ? ZoneId.of("UTC") : ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("unknown time zone: " + zoneId);
        }
        return new CronSchedule(CronExpression.parse(sixFields), zone);
    }

    /** The first occurrence strictly after {@code after}. */
    public Instant next(Instant after) {
        ZonedDateTime from = after.atZone(zone);
        ZonedDateTime next = expression.next(from);
        // When clocks go back an hour, a wall-clock time happens twice. Fire for the first one only.
        while (next != null && next.toLocalDateTime().equals(from.toLocalDateTime())) {
            next = expression.next(next);
        }
        if (next == null) {
            throw new IllegalArgumentException("cron expression has no further occurrence");
        }
        return next.toInstant();
    }

    /** Smallest gap among the next few occurrences after {@code from}; used to refuse too-frequent timers. */
    public Duration shortestPeriod(Instant from) {
        Duration shortest = null;
        Instant previous = next(from);
        for (int i = 0; i < 6; i++) {
            Instant following = next(previous);
            Duration gap = Duration.between(previous, following);
            if (shortest == null || gap.compareTo(shortest) < 0) {
                shortest = gap;
            }
            previous = following;
        }
        return shortest;
    }

    public ZoneId zone() {
        return zone;
    }
}
