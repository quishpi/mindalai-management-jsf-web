package ec.mindalai.managementjsf.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;

/**
 * Cliente HTTP de salida. Spring Boot 4 configura por defecto los conversores Jackson 3
 * sobre el contexto (por eso se reutiliza el builder global) y aqui solo se fijan la base URL
 * y las timeouts de
 * {@link AppProperties} a cada llamada remota (Regla 15).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final AppProperties appProperties;

    @Bean
    RestClient restClient() {
        return RestClient.builder()
                .baseUrl(appProperties.getBaseUrl())
                .requestFactory(requestFactory())
                .build();
    }

    private org.springframework.http.client.JdkClientHttpRequestFactory requestFactory() {
        var timeout = appProperties.effectiveTimeout();
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);
        return factory;
    }
}
