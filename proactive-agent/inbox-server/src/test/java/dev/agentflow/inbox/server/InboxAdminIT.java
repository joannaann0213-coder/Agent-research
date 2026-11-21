package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.inbox.api.AuditEntry;
import dev.agentflow.inbox.api.ClaimedRun;
import dev.agentflow.inbox.api.EventSource;
import dev.agentflow.inbox.api.InboxConfigRequest;
import dev.agentflow.inbox.api.InboxStatus;
import dev.agentflow.inbox.api.Lane;
import dev.agentflow.inbox.api.NackRequest;
import dev.agentflow.inbox.api.ProblemCodes;
import dev.agentflow.inbox.api.WriteEventRequest;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.client.InboxClient;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Operators can see what is waiting in an inbox and change its limits; every change is audited. */
class InboxAdminIT extends IntegrationTestBase {

    private static WriteEventRequest.Builder on(Lane lane, String key) {
        return WriteEventRequest.builder(EventSource.BIZ_WEBHOOK, "job.requested.v1")
                .sourceEventId(key).lane(lane).payloadJson("{}");
    }

    @Test
    void statusShowsWhatIsWaitingAndWhere() {
        write("u42", on(Lane.INTERACTIVE, "i1"));
        write("u42", on(Lane.INTERACTIVE, "i2"));
        write("u42", on(Lane.BACKGROUND, "b1"));
        write("u42", on(Lane.BATCH, "x1"));
        ClaimedRun interactive = claim("worker-1", "u42").require();   // i1 is claimed
        ClaimedRun background = claim("worker-2", "u42").require();    // b1 is claimed, then nacked
        runtime.nack(background.runId(), new NackRequest(background.epoch(), "try later", null)).require();

        InboxStatus status = ops.inboxStatus(ACME, "u42").require();
        assertThat(status.pending()).as("i2 and x1 wait unclaimed").isEqualTo(2);
        assertThat(status.retrying()).as("b1 waits with its queued run").isEqualTo(1);
        assertThat(status.claimed()).isEqualTo(1);
        assertThat(status.dead()).isZero();
        assertThat(status.activeRuns()).isEqualTo(1);
        assertThat(status.queuedRuns()).isEqualTo(1);
        assertThat(status.suspendedRuns()).isZero();
        assertThat(status.maxActiveRuns()).as("no override yet").isNull();

        assertThat(ops.inboxStatus(GLOBEX, "u42").status()).isEqualTo(404);
        runtime.ackDone(interactive).require();
    }

    @Test
    void limitsCanBeChangedPerInbox_andTheChangeIsAudited() {
        write("u42", on(Lane.INTERACTIVE, "i1"));
        write("u42", on(Lane.BACKGROUND, "b1"));

        InboxStatus configured = ops.configureInbox("u42", new InboxConfigRequest(ACME, null, 1, null, 1,
                InboxClient_json("{\"lanes\":{\"interactive\":{\"quiet_window_ms\":500}}}"), "alice")).require();
        assertThat(configured.maxActiveRuns()).isEqualTo(1);
        assertThat(configured.capBackground()).isEqualTo(1);
        assertThat(configured.settings().get("lanes").get("interactive").get("quiet_window_ms").intValue()).isEqualTo(500);

        // strict mode is in force: one run per inbox, whatever the lane
        ClaimedRun first = claim("worker-1", "u42").require();
        assertThat(claim("worker-2", "u42").status()).isEqualTo(204);
        runtime.ackDone(first).require();
        runtime.ackDone(claim("worker-2", "u42").require()).require();

        // the shorter quiet window is in force too
        write("u42", message("m1", "u42", "hello"));
        clock.advance(Duration.ofMillis(500));
        runtime.ackDone(claim("worker-1", "u42").require()).require();

        AuditEntry entry = ops.audit(ACME, 1).require()[0];
        assertThat(entry.action()).isEqualTo("inbox_config");
        assertThat(entry.operator()).isEqualTo("alice");
        assertThat(entry.target().get("inbox_id").stringValue()).isEqualTo("u42");
        assertThat(entry.params().get("max_active_runs").intValue()).isEqualTo(1);

        // sending the config again without a field resets that field to the default
        InboxStatus reset = ops.configureInbox("u42", new InboxConfigRequest(ACME, null, null, null, null, null, "alice")).require();
        assertThat(reset.maxActiveRuns()).isNull();
        assertThat(reset.capBackground()).isNull();
    }

    @Test
    void anInboxCanBeConfiguredBeforeItsFirstEvent() {
        ops.configureInbox("new-user", new InboxConfigRequest(ACME, null, null, null, 1, null, null)).require();
        write("new-user", on(Lane.INTERACTIVE, "i1"));
        assertThat(ops.inboxStatus(ACME, "new-user").require().pending()).isEqualTo(1);
        assertThat(acme.getEvent("new-user", acme.listEvents("new-user", "").require()[0].eventId()).require()
                .event().seq()).isEqualTo(1);
    }

    @Test
    void invalidConfigurationIsRefused() {
        assertThat(ops.configureInbox("u42", new InboxConfigRequest(ACME, 0, null, null, null, null, null)).status()).isEqualTo(400);
        assertThat(ops.configureInbox("u42", new InboxConfigRequest(ACME, null, null, null, 500, null, null)).status()).isEqualTo(400);
        assertThat(ops.configureInbox("u42", new InboxConfigRequest(ACME, null, null, null, null,
                InboxClient_json("[1,2]"), null)).status()).isEqualTo(400);
        assertThat(ops.configureInbox("u42", new InboxConfigRequest(null, null, null, null, 1, null, null)).status()).isEqualTo(400);
        assertThat(acme.put("/v1/admin/inboxes/u42/config", new InboxConfigRequest(ACME, null, null, null, 1, null, null),
                InboxStatus.class).status()).as("producers are not operators").isEqualTo(403);
        assertThat(count("SELECT count(*) FROM inbox")).isZero();
    }

    @Test
    void settingsWithAnUnknownKeyOrAValueOutOfRangeAreRefused() {
        String[] refused = {
                "{\"lanes\":{\"interactive\":{\"quiet_windw_ms\":3000}}}",
                "{\"lanes\":{\"urgent\":{\"quiet_window_ms\":3000}}}",
                "{\"lanes\":{\"interactive\":{\"max_events\":20}}}",
                "{\"lanes\":{\"interactive\":{\"quiet_window_ms\":\"3s\"}}}",
                "{\"lanes\":{\"interactive\":{\"quiet_window_ms\":null}}}",
                "{\"lanes\":{\"interactive\":{\"max_wait_ms\":-1}}}",
                "{\"lanes\":{\"interactive\":{\"quiet_window_ms\":86400001}}}",
                "{\"batch\":{\"max_events\":0}}",
                "{\"batch\":{\"max_events\":2.5}}",
                "{\"windows\":{}}",
        };
        for (String settings : refused) {
            ApiResult<InboxStatus> answer = ops.configureInbox("u42",
                    new InboxConfigRequest(ACME, null, null, null, null, InboxClient_json(settings), "alice"));
            assertThat(answer.status()).as(settings).isEqualTo(400);
            assertThat(answer.code()).as(settings).isEqualTo(ProblemCodes.INVALID_REQUEST);
        }
        ApiResult<InboxStatus> typo = ops.configureInbox("u42", new InboxConfigRequest(ACME, null, null, null, null,
                InboxClient_json("{\"lanes\":{\"interactive\":{\"quiet_windw_ms\":3000}}}"), "alice"));
        assertThat(typo.problem().detail()).as("the error names the key").contains("lanes.interactive.quiet_windw_ms");
        assertThat(count("SELECT count(*) FROM inbox")).as("nothing was stored").isZero();
        assertThat(count("SELECT count(*) FROM admin_audit")).isZero();

        // every known key, at the edges of its range, is accepted
        InboxStatus accepted = ops.configureInbox("u42", new InboxConfigRequest(ACME, null, null, null, null,
                InboxClient_json("{\"lanes\":{\"interactive\":{\"quiet_window_ms\":0,\"max_wait_ms\":1000},"
                        + "\"background\":{\"quiet_window_ms\":30000},\"batch\":{\"max_wait_ms\":86400000}},"
                        + "\"batch\":{\"max_events\":1,\"max_bytes\":1024}}"), "alice")).require();
        assertThat(accepted.settings().get("batch").get("max_events").intValue()).isEqualTo(1);
    }

    private static tools.jackson.databind.JsonNode InboxClient_json(String json) {
        return dev.agentflow.inbox.api.InboxJson.mapper().readTree(json);
    }
}
