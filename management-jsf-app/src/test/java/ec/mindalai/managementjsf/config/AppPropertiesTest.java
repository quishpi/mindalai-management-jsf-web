package ec.mindalai.managementjsf.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppPropertiesTest {

    @Test
    void authTokenUrlDerivesFromBaseUrl() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:9094");
        assertEquals("http://localhost:9094/api/v1.0/platform/auth/login", props.getAuthTokenUrl());
    }

    @Test
    void authTokenUrlStripsTrailingSlash() {
        AppProperties props = new AppProperties();
        props.setBaseUrl("http://localhost:9094/");
        assertEquals("http://localhost:9094/api/v1.0/platform/auth/login", props.getAuthTokenUrl());
    }
}