package ec.mindalai.managementjsf.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** Configuracion del consumidor: endpoint del API de plataforma y timeouts de salida. */
@Component
@ConfigurationProperties(prefix = "app.api")
@Getter
@Setter
@Validated
public class AppProperties {

    /** Base del API, por ejemplo {@code http://localhost:9094}. */
    @NotBlank
    private String baseUrl = "http://localhost:9094";

    /** Timeout de conexion y de lectura hacia el API. */
    private Duration timeout = Duration.ofSeconds(5);

    /** Timeout efectivo, siempre positivo (Regla 15). */
    public Duration effectiveTimeout() {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return Duration.ofSeconds(5);
        }
        return timeout;
    }

    public String platformUrl(String path) {
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        String suffix = path.startsWith("/") ? path : "/" + path;
        return base + "/api/v1.0/platform" + suffix;
    }

    public String authLoginUrl() {
        return platformUrl("/auth/login");
    }
}
