package ec.mindalai.managementjsf.client;

import ec.mindalai.managementjsf.dto.ApiError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformApiExceptionTest {

    private static PlatformApiException exception(int status, String code) {
        return new PlatformApiException(ApiError.builder()
                .status(status)
                .code(code)
                .message("mensaje del backend")
                .build());
    }

    @Test
    void detectsUnauthorizedByStatus() {
        assertThat(exception(401, "UNAUTHORIZED").isUnauthorized()).isTrue();
    }

    @Test
    void detectsUnauthorizedByCode() {
        assertThat(exception(0, "INVALID_TOKEN").isUnauthorized()).isTrue();
    }

    @Test
    void forbiddenIsNotUnauthorized() {
        PlatformApiException ex = exception(403, "FORBIDDEN");

        assertThat(ex.isUnauthorized()).isFalse();
        assertThat(ex.isForbidden()).isTrue();
    }

    @Test
    void detectsConflictByStatusOrCode() {
        assertThat(exception(409, "CONFLICT").isConflict()).isTrue();
        assertThat(exception(0, "CONFLICT").isConflict()).isTrue();
        assertThat(exception(400, "VALIDATION_ERROR").isConflict()).isFalse();
    }

    @Test
    void conflictKeepsDetailCodeSentAsText() {
        PlatformApiException ex = new PlatformApiException(ApiError.builder()
                .status(409)
                .code("CONFLICT")
                .message("Ya existe un tenant con el RUC 1790012345001")
                .details("TENANT_DUPLICATED_RUC")
                .build());

        assertThat(ex.isConflict()).isTrue();
        assertThat(ex.getError().hasDetails()).isTrue();
        assertThat(ex.getError().detailText()).isEqualTo("TENANT_DUPLICATED_RUC");
    }

    @Test
    void keepsCorrelationIdForSupport() {
        PlatformApiException ex = new PlatformApiException(ApiError.builder()
                .status(409)
                .code("CONFLICT")
                .message("RUC ya registrado")
                .correlationId("6b0c1a5e")
                .build());

        assertThat(ex.getMessage()).isEqualTo("RUC ya registrado");
        assertThat(ex.getCode()).isEqualTo("CONFLICT");
        assertThat(ex.getCorrelationId()).isEqualTo("6b0c1a5e");
    }

    @Test
    void fallsBackToGenericMessageWhenBackendOmitsIt() {
        PlatformApiException ex = new PlatformApiException(ApiError.builder().build());

        assertThat(ex.getMessage()).isNotBlank();
    }
}
