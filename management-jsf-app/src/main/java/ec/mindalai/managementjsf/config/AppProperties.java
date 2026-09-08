package ec.mindalai.managementjsf.config;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix="app.api")
@Getter @Setter
public class AppProperties {
    private String baseUrl = "http://localhost:8082";
    private String authTokenUrl = "http://localhost:8082/api/v1.0/platform/auth/login";
    private int timeout = 5000;
}
