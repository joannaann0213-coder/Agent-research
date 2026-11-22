package dev.agentflow.agent.server.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import dev.agentflow.agent.server.config.AgentProperties;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Which addresses a webhook may reach: the public internet, and nothing behind the service. */
class AddressPolicyTest {

    private final AddressPolicy policy = new AddressPolicy(new AgentProperties());

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "10.0.0.1", "10.255.255.255", "100.64.0.1", "100.127.255.255", "127.0.0.1", "127.1.2.3",
            "169.254.169.254", "169.254.0.1", "172.16.0.1", "172.31.255.255", "192.0.0.8", "192.0.2.10", "192.168.1.1",
            "198.18.0.1", "198.19.255.255", "198.51.100.7", "203.0.113.9", "224.0.0.1", "239.255.255.250", "240.0.0.1",
            "255.255.255.255", "::", "::1", "fc00::1", "fd00:ec2::254", "fe80::1", "ff02::1", "2001:db8::1",
            "::ffff:127.0.0.1", "::ffff:10.0.0.1", "::ffff:169.254.169.254", "64:ff9b::a9fe:a9fe", "::10.0.0.1"})
    void addressesBehindTheServiceAreRefused(String address) throws UnknownHostException {
        assertThat(policy.refusal(InetAddress.getByName(address))).as(address).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "172.32.0.1", "172.15.255.255", "100.128.0.1", "100.63.255.255", "169.255.0.1",
            "198.20.0.1", "93.184.216.34", "2606:4700:4700::1111", "::ffff:8.8.8.8", "64:ff9b::808:808"})
    void publicAddressesMayBeReached(String address) throws UnknownHostException {
        assertThat(policy.refusal(InetAddress.getByName(address))).as(address).isNull();
    }

    @Test
    void theMetadataServiceIsNamedForWhatItIs() throws UnknownHostException {
        assertThat(policy.refusal(InetAddress.getByName("169.254.169.254"))).isEqualTo("link-local, where cloud metadata services are");
        assertThat(policy.refusal(InetAddress.getByName("127.0.0.1"))).isEqualTo("the host itself");
    }

    @Test
    void aWebhookURLMustBeHttps_toAPublicHost_withoutUserInformation() {
        assertThat(policy.problemWith("http://203.0.113.5/hook")).isEqualTo("url must be an https URL");
        assertThat(policy.problemWith("ftp://example.com/hook")).isEqualTo("url must be an https URL");
        assertThat(policy.problemWith("https://user:secret@8.8.8.8/hook")).startsWith("url needs a host");
        assertThat(policy.problemWith("https://8.8.8.8/hook#fragment")).startsWith("url needs a host");
        assertThat(policy.problemWith("https://127.0.0.1/hook")).contains("the host itself");
        assertThat(policy.problemWith("https://localhost:8443/hook")).contains("the host itself");
        assertThat(policy.problemWith("https://[::1]/hook")).contains("the host itself");
        assertThat(policy.problemWith("https://169.254.169.254/latest/meta-data/")).contains("cloud metadata");
        assertThat(policy.problemWith("https://10.1.2.3/hook")).contains("not a public address");
        assertThat(policy.problemWith("not a url at all")).isNotNull();
        assertThat(policy.problemWith(null)).isNotNull();
        assertThat(policy.problemWith("https://8.8.8.8/hook")).isNull();
    }

    @Test
    void privateNetworksAndPlainHttpCanBeAllowed_forTestsAndLocalDevelopment() throws UnknownHostException {
        AgentProperties properties = new AgentProperties();
        properties.getWebhooks().setAllowPrivateNetworks(true);
        properties.getWebhooks().setAllowPlainHttp(true);
        AddressPolicy relaxed = new AddressPolicy(properties);

        assertThat(relaxed.refusal(InetAddress.getByName("127.0.0.1"))).isNull();
        assertThat(relaxed.problemWith("http://127.0.0.1:8080/hook")).isNull();
    }
}
