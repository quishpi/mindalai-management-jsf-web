package ec.mindalai.managementjsf.client;

import ec.mindalai.managementjsf.dto.ApiError;

/**
 * Error normalizado recebido del API de plataforma. Se traduce a mensaje de UI sin
 * exponer stack traces ni credenciales al operador.
 */
public class PlatformApiException extends RuntimeException {

    private final transient ApiError error;

    public PlatformApiException(ApiError error) {
        super(error != null && error.getMessage() != null ? error.getMessage() : "Error en el API de plataforma");
        this.error = error != null ? error : ApiError.builder().message(getMessage()).build();
    }

    public static PlatformApiException of(String code, String message, String correlationId) {
        return new PlatformApiException(ApiError.builder()
                .code(code)
                .message(message)
                .correlationId(correlationId)
                .build());
    }

    public ApiError getError() {
        return error;
    }

    public String getCode() {
        return error.getCode();
    }

    public String getCorrelationId() {
        return error.getCorrelationId();
    }

    public boolean isUnauthorized() {
        return error.getStatus() == 401 || "UNAUTHORIZED".equals(error.getCode()) || "INVALID_TOKEN".equals(error.getCode());
    }

    public boolean isForbidden() {
        return error.getStatus() == 403 || "FORBIDDEN".equals(error.getCode());
    }

    public boolean isConflict() {
        return error.getStatus() == 409 || "CONFLICT".equals(error.getCode());
    }
}
