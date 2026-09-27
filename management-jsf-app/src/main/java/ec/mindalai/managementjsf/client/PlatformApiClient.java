package ec.mindalai.managementjsf.client;

import ec.mindalai.managementjsf.dto.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

/**
 * Cliente base del API de plataforma. Centraliza la cabecera {@code Authorization},
 * la propagacion de {@code X-Correlation-Id}, los timeouts y el mapeo de los errores JSON
 * del backend a {@link PlatformApiException}. No reintenta: idempotencia y reintentos son
 * responsabilidad del backend (Regla 15).
 */
@Slf4j
@Component
public class PlatformApiClient {

    private static final String PLATFORM = "/api/v1.0/platform";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PlatformApiClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public static String platformPath(String path) {
        return PLATFORM + (path.startsWith("/") ? path : "/" + path);
    }

    /** Construye un cuerpo JSON omitiendo los pares con valor {@code null} (semantica de actualizacion parcial). */
    public static Map<String, Object> body(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("body() requiere pares clave/valor");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            if (value != null) {
                map.put(String.valueOf(keyValues[i]), value);
            }
        }
        return map;
    }

    public <T> T get(String path, String token, String correlationId, Class<T> type) {
        return get(path, token, correlationId, type, Map.of());
    }

    public <T> T get(String path, String token, String correlationId, Class<T> type, Map<String, Object> query) {
        return call("GET", path, token, correlationId, query, null, type);
    }

    /** Lista a partir de la respuesta JSON del backend, que siempre es un array plano. */
    public <T> List<T> getList(String path, String token, String correlationId, Class<T[]> arrayType,
                               Map<String, Object> query) {
        T[] array = call("GET", path, token, correlationId, query, null, arrayType);
        return array == null ? List.of() : Arrays.asList(array);
    }

    public <T> T get(String path, String token, String correlationId, ParameterizedTypeReference<T> type,
                     Map<String, Object> query) {
        try {
            return request("GET", path, token, correlationId, query, null).retrieve().body(type);
        } catch (RestClientResponseException ex) {
            throw new PlatformApiException(parseError(ex, correlationId));
        } catch (ResourceAccessException ex) {
            throw transportFailure(path, "GET", correlationId, ex);
        }
    }

    public <T> T post(String path, String token, String correlationId, Object body, Class<T> type) {
        return call("POST", path, token, correlationId, Map.of(), body, type);
    }

    /**
     * POST con parametros de query y sin cuerpo. Lo exigen los endpoints del API que declaran
     * {@code @RequestParam} en lugar de {@code @RequestBody} (por ejemplo
     * {@code /installations/{id}/maintenance?value=} o {@code /support/tickets/{id}/assign?operatorId=}).
     */
    public <T> T postWithQuery(String path, String token, String correlationId, Map<String, Object> query,
                               Class<T> type) {
        return call("POST", path, token, correlationId, query, null, type);
    }

    public <T> T put(String path, String token, String correlationId, Object body, Class<T> type) {
        return call("PUT", path, token, correlationId, Map.of(), body, type);
    }

    public <T> T delete(String path, String token, String correlationId, Class<T> type) {
        return call("DELETE", path, token, correlationId, Map.of(), null, type);
    }

    private <T> T call(String method, String path, String token, String correlationId, Map<String, Object> query,
                       Object body, Class<T> type) {
        try {
            return request(method, path, token, correlationId, query, body).retrieve().body(type);
        } catch (RestClientResponseException ex) {
            throw new PlatformApiException(parseError(ex, correlationId));
        } catch (ResourceAccessException ex) {
            throw transportFailure(path, method, correlationId, ex);
        }
    }

    private RestClient.RequestBodySpec request(String method, String path, String token, String correlationId,
                                              Map<String, Object> query, Object body) {
        RestClient.RequestBodySpec spec = restClient.method(HttpMethod.valueOf(method))
                .uri(uri(path, query))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (correlationId != null && !correlationId.isBlank()) {
            spec.header("X-Correlation-Id", correlationId);
        }
        if (token != null && !token.isBlank()) {
            spec.header("Authorization", "Bearer " + token);
        }
        return body == null ? spec : spec.body(body);
    }

    private PlatformApiException transportFailure(String path, String method, String correlationId,
                                                  ResourceAccessException ex) {
        log.warn("Fallo de transporte hacia {} ({}): {}", path, method, ex.getMessage());
        return PlatformApiException.of("SERVICE_UNAVAILABLE",
                "El servicio de plataforma no esta disponible", correlationId);
    }

    private ApiError parseError(RestClientResponseException ex, String correlationId) {
        HttpStatusCode status = ex.getStatusCode();
        ApiError parsed = null;
        try {
            parsed = objectMapper.readValue(ex.getResponseBodyAsString(), ApiError.class);
        } catch (Exception ignored) {
            log.debug("Respuesta de error no JSON ({}): {}", status, ex.getResponseHeaders());
        }
        ApiError error = parsed != null ? parsed : ApiError.builder().build();
        if (error.getStatus() == 0) {
            error.setStatus(status.value());
        }
        if (error.getMessage() == null || error.getMessage().isBlank()) {
            error.setMessage("Error " + status.value());
        }
        if (error.getCorrelationId() == null) {
            error.setCorrelationId(correlationId);
        }
        return error;
    }

    private URI uri(String path, Map<String, Object> query) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(path);
        if (query != null) {
            query.forEach((key, value) -> {
                if (value != null && !String.valueOf(value).isBlank()) {
                    uriBuilder.queryParam(key, value instanceof Instant instant ? instant.toString() : value);
                }
            });
        }
        // build(true) exige valores ya codificados y falla con IllegalArgumentException ante
        // texto libre (por ejemplo la resolucion de un ticket: "Invalid character ' ' for
        // QUERY_PARAM"). Se construye sin marcar y se codifica la query.
        return uriBuilder.build().encode().toUri();
    }
}
