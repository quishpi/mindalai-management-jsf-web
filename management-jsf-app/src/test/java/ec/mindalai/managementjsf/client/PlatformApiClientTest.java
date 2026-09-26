package ec.mindalai.managementjsf.client;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PlatformApiClientTest {

    @Test
    void platformPathAlwaysUsesTheVersionedPlatformPrefix() {
        assertThat(PlatformApiClient.platformPath("/tenants"))
                .isEqualTo("/api/v1.0/platform/tenants");
        assertThat(PlatformApiClient.platformPath("tenants"))
                .isEqualTo("/api/v1.0/platform/tenants");
    }

    @Test
    void bodyOmitsNullPairs() {
        Map<String, Object> body = PlatformApiClient.body(
                "legalName", "ACME",
                "ruc", "1790012345001",
                "email", null);

        assertThat(body).containsOnlyKeys("legalName", "ruc");
        assertThat(body).containsEntry("legalName", "ACME");
    }

    @Test
    void bodyRejectsOddNumberOfArguments() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> PlatformApiClient.body("legalName"));
    }

    @Test
    void bodyPreservesInsertionOrder() {
        Map<String, Object> body = PlatformApiClient.body("a", 1, "b", 2, "c", 3);

        assertThat(new java.util.ArrayList<>(body.keySet()))
                .isEqualTo(java.util.List.of("a", "b", "c"));
    }

    @Test
    void emptyBodyProducesEmptyMap() {
        assertThat(PlatformApiClient.body()).isEqualTo(new LinkedHashMap<String, Object>());
    }
}
