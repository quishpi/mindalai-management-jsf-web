package ec.mindalai.managementjsf.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    @Test
    void platformUrlDerivesFromBaseUrl() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:9094");

        assertThat(props.platformUrl("/auth/login"))
                .isEqualTo("http://localhost:9094/api/v1.0/platform/auth/login");
    }

    @Test
    void platformUrlStripsTrailingSlash() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:9094/");

        assertThat(props.authLoginUrl())
                .isEqualTo("http://localhost:9094/api/v1.0/platform/auth/login");
    }
}
