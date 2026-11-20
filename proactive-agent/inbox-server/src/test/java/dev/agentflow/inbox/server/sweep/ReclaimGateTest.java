package dev.agentflow.inbox.server.sweep;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReclaimGateTest {

    private static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");
    private static final Duration LEASE = Duration.ofSeconds(60);

    @Test
    void holdsBackForOneLeaseAfterAStart() {
        ReclaimGate gate = new ReclaimGate(LEASE, T0);
        assertThat(gate.mayReclaim(T0.plusSeconds(1))).isFalse();
        assertThat(gate.mayReclaim(T0.plusSeconds(59))).isFalse();
        assertThat(gate.mayReclaim(T0.plusSeconds(60))).isTrue();
        assertThat(gate.mayReclaim(T0.plusSeconds(61))).isTrue();
    }

    @Test
    void holdsBackAgainAfterAFailedPass() {
        ReclaimGate gate = new ReclaimGate(LEASE, T0);
        assertThat(gate.mayReclaim(T0.plusSeconds(60))).isTrue();
        gate.passFailed(T0.plusSeconds(61));
        assertThat(gate.mayReclaim(T0.plusSeconds(62))).isFalse();
        assertThat(gate.mayReclaim(T0.plusSeconds(120))).isFalse();
        assertThat(gate.mayReclaim(T0.plusSeconds(121))).isTrue();
    }

    @Test
    void holdsBackWhenTheLoopItselfStalled() {
        ReclaimGate gate = new ReclaimGate(LEASE, T0);
        assertThat(gate.mayReclaim(T0.plusSeconds(60))).isTrue();
        // nothing ran for five minutes (a database failover, a long pause): leases only look expired
        Instant back = T0.plusSeconds(360);
        assertThat(gate.mayReclaim(back)).isFalse();
        assertThat(gate.mayReclaim(back.plusSeconds(30))).isFalse();
        assertThat(gate.mayReclaim(back.plusSeconds(60))).isTrue();
    }
}
