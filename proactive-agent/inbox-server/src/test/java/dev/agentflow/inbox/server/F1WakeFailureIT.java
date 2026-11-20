package dev.agentflow.inbox.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import dev.agentflow.inbox.api.WriteEventResponse;
import dev.agentflow.inbox.client.ApiResult;
import dev.agentflow.inbox.server.support.IntegrationTestBase;
import dev.agentflow.inbox.server.wake.WakeBus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * F1: write latency and availability are decoupled from everything downstream. Waking the runtime is
 * a hint sent after the write; when it fails, the write is still accepted.
 */
class F1WakeFailureIT extends IntegrationTestBase {

    @MockitoBean
    WakeBus wakeBus;

    @Test
    void acceptsEvenWhenTheWakeChannelIsBroken() {
        doThrow(new IllegalStateException("wake channel is down")).when(wakeBus).publish();

        ApiResult<WriteEventResponse> result = acme.writeEvent("u42", webhook("order-1", "{}").build());

        assertThat(result.status()).isEqualTo(202);
        assertThat(count("SELECT count(*) FROM event WHERE status = 'pending'")).isEqualTo(1);
        verify(wakeBus, atLeastOnce()).publish();
    }
}
