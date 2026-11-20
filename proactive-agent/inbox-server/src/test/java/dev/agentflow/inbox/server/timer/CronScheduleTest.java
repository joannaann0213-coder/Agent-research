package dev.agentflow.inbox.server.timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CronScheduleTest {

    private static List<Instant> occurrences(CronSchedule schedule, Instant from, int count) {
        List<Instant> result = new ArrayList<>();
        Instant cursor = from;
        for (int i = 0; i < count; i++) {
            cursor = schedule.next(cursor);
            result.add(cursor);
        }
        return result;
    }

    @Test
    void nineInTheMorningIsTheUsersNineInTheMorning() {
        CronSchedule schedule = CronSchedule.parse("0 9 * * *", "Asia/Shanghai");
        // 2026-11-20 10:00 UTC is 18:00 in Shanghai: the next 9 am there is 01:00 UTC on the 21st.
        assertThat(schedule.next(Instant.parse("2026-11-20T10:00:00Z"))).isEqualTo(Instant.parse("2026-11-21T01:00:00Z"));
        assertThat(schedule.next(Instant.parse("2026-11-21T01:00:00Z"))).isEqualTo(Instant.parse("2026-11-22T01:00:00Z"));
    }

    @Test
    void fiveFieldsAndSixFieldsAreBothAccepted_andUtcIsTheDefaultZone() {
        Instant from = Instant.parse("2026-11-20T10:00:00Z");
        assertThat(CronSchedule.parse("0 9 * * *", null).next(from))
                .isEqualTo(CronSchedule.parse("0 0 9 * * *", "UTC").next(from))
                .isEqualTo(Instant.parse("2026-11-21T09:00:00Z"));
    }

    @Test
    void whenClocksGoBack_aWallClockTimeThatHappensTwiceFiresOnce() {
        // New York leaves daylight time on 2026-11-01: 01:30 happens twice that night.
        ZoneId zone = ZoneId.of("America/New_York");
        CronSchedule schedule = CronSchedule.parse("30 1 * * *", zone.getId());
        List<Instant> fired = occurrences(schedule, Instant.parse("2026-10-30T12:00:00Z"), 4);

        assertThat(fired).isSorted().doesNotHaveDuplicates();
        assertThat(fired).extracting(i -> i.atZone(zone).toLocalDate()).containsExactly(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3));
        assertThat(fired).allSatisfy(i -> assertThat(i.atZone(zone).toLocalTime().toString()).isEqualTo("01:30"));
    }

    @Test
    void whenClocksGoForward_theSeriesContinuesWithAtMostOneFiringPerDay() {
        // New York enters daylight time on 2026-03-08: 02:30 does not exist that night.
        ZoneId zone = ZoneId.of("America/New_York");
        CronSchedule schedule = CronSchedule.parse("30 2 * * *", zone.getId());
        List<Instant> fired = occurrences(schedule, Instant.parse("2026-03-06T12:00:00Z"), 4);

        assertThat(fired).isSorted().doesNotHaveDuplicates();
        assertThat(fired).extracting(i -> i.atZone(zone).toLocalDate()).doesNotHaveDuplicates();
        assertThat(fired.getFirst().atZone(zone).toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 7));
        assertThat(Duration.between(fired.getFirst(), fired.getLast())).isLessThanOrEqualTo(Duration.ofDays(4));
    }

    @Test
    void shortestPeriodSeesThroughIrregularExpressions() {
        Instant from = Instant.parse("2026-11-20T10:00:00Z");
        assertThat(CronSchedule.parse("*/30 * * * * *", null).shortestPeriod(from)).isEqualTo(Duration.ofSeconds(30));
        assertThat(CronSchedule.parse("0 * * * *", null).shortestPeriod(from)).isEqualTo(Duration.ofHours(1));
        assertThat(CronSchedule.parse("0,1 9 * * *", null).shortestPeriod(from)).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void invalidInputIsRefused() {
        assertThatThrownBy(() -> CronSchedule.parse("every day at nine", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CronSchedule.parse("61 * * * *", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CronSchedule.parse("", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CronSchedule.parse("0 9 * * *", "Mars/Olympus")).isInstanceOf(IllegalArgumentException.class);
    }
}
