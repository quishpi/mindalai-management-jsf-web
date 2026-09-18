package ec.mindalai.managementjsf.config;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component @ConfigurationProperties(prefix="app.api")
@Getter @Setter
public class AppProperties {
    private String baseUrl;
    private int timeout;

    public String getAuthTokenUrl() {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return base + "/api/v1.0/platform/auth/login";
    }
}