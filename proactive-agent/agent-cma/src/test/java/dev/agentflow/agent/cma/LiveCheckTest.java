package dev.agentflow.agent.cma;

import static dev.agentflow.agent.cma.fake.ModelStep.builtin;
import static dev.agentflow.agent.cma.fake.ModelStep.custom;
import static dev.agentflow.agent.cma.fake.ModelStep.say;
import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.cma.fake.FakeManagedAgents;
import dev.agentflow.agent.cma.fake.ModelStep;
import dev.agentflow.agent.cma.live.LiveCheck;
import dev.agentflow.agent.cma.live.LiveCheck.Finding;
import dev.agentflow.agent.cma.live.LiveCheck.Verdict;
import dev.agentflow.agent.cma.sdk.SdkManagedAgents;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The live check, run against the fake. The fake is built on the assumptions the live check tests,
 * so here every verdict has to be "as assumed"; a verdict that is not means the two have drifted
 * apart. Run against the real service, the same program says whether the assumptions hold.
 */
class LiveCheckTest {

    private static final LiveCheck.Options FAST =
            new LiveCheck.Options("claude-opus-5-5", true, Duration.ofSeconds(10), Duration.ofMillis(300));

    /** Plays the model the way the prompts of the live check ask it to behave. */
    private static ModelStep fixture(dev.agentflow.agent.cma.fake.ModelContext context) {
        String message = context.userMessage();
        if (message.contains("probe_echo")) {
            return context.step() == 0
                    ? ModelStep.of(custom("probe_echo", message.contains("ping") ? "{\"text\": \"ping\"}" : "{\"text\": \"wait\"}"))
                    : say("It returned " + context.resultOf("probe_echo").text() + ".");
        }
        if (message.contains("bash")) {
            return context.step() == 0
                    ? ModelStep.of(builtin("bash", "{\"command\": \"echo one\"}"), builtin("bash", "{\"command\": \"echo two\"}"))
                    : say("DONE");
        }
        return say(message.contains("READY") ? "READY" : "AGAIN");
    }

    @Test
    void onTheFakeEveryAssumptionHolds() {
        FakeManagedAgents fake = new FakeManagedAgents();
        fake.brain(LiveCheckTest::fixture);
        ByteArrayOutputStream printed = new ByteArrayOutputStream();

        List<Finding> findings = new LiveCheck(fake, FAST, new PrintStream(printed, true, StandardCharsets.UTF_8)).run();

        assertThat(findings).as(printed.toString(StandardCharsets.UTF_8)).hasSizeGreaterThan(15)
                .allSatisfy(finding -> assertThat(finding.verdict()).as(finding.check() + ": " + finding.detail())
                        .isEqualTo(Verdict.AS_ASSUMED));
        assertThat(printed.toString(StandardCharsets.UTF_8)).contains("Summary", "Sessions left in place");
        assertThat(fake.sessionIds()).as("the sessions are left for inspection").hasSize(2);
    }

    @Test
    void throughTheSdkAdapterItTellsTheSameStory() {
        FakeManagedAgents fake = new FakeManagedAgents();
        fake.brain(LiveCheckTest::fixture);
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        try (CmaWireStub stub = new CmaWireStub(fake);
                SdkManagedAgents cma = SdkManagedAgents.connect("sk-test-key", stub.baseUrl(), Duration.ofSeconds(20))) {

            List<Finding> findings = new LiveCheck(cma, FAST, new PrintStream(printed, true, StandardCharsets.UTF_8)).run();

            assertThat(findings).as(printed.toString(StandardCharsets.UTF_8))
                    .allSatisfy(finding -> assertThat(finding.verdict()).as(finding.check() + ": " + finding.detail())
                            .isEqualTo(Verdict.AS_ASSUMED));
        }
    }

    @Test
    void aServiceThatBehavesDifferentlyIsReported_andASkippedCheckIsNotAFailure() {
        FakeManagedAgents fake = new FakeManagedAgents();
        // a model that never calls the tool it is asked to call
        fake.brain(context -> say("I would rather not."));
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        LiveCheck.Options noSandbox = new LiveCheck.Options("claude-opus-5-5", false, Duration.ofSeconds(5), Duration.ofMillis(200));

        List<Finding> findings = new LiveCheck(fake, noSandbox, new PrintStream(printed, true, StandardCharsets.UTF_8)).run();

        assertThat(findings).extracting(Finding::verdict).contains(Verdict.DIFFERENT, Verdict.NOT_OBSERVED, Verdict.AS_ASSUMED);
        assertThat(findings).filteredOn(finding -> finding.check().equals("tool confirmations")).singleElement()
                .satisfies(finding -> assertThat(finding.verdict()).isEqualTo(Verdict.NOT_OBSERVED));
        assertThat(findings).filteredOn(finding -> finding.check().startsWith("a custom tool call stops")).singleElement()
                .satisfies(finding -> assertThat(finding.verdict()).isEqualTo(Verdict.DIFFERENT));
    }
}
