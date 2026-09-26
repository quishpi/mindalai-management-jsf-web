package ec.mindalai.managementjsf.security;

import ec.mindalai.managementjsf.client.AuthRestClient;
import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Fachada de seguridad de la vista: entrega el access token vigente al resto de la aplicacion
 * y lo renueva de forma transparente con el refresh token. Si el refresh falla, la sesion
 * se invalida y el operador vuelve al login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformSecurity {

    private final AuthRestClient authRestClient;
    private final AuthSession authSession;

    public String accessToken() {
        if (!authSession.isAuthenticated()) {
            throw PlatformApiException.of("UNAUTHORIZED", "Sesion no iniciada", null);
        }
        if (!authSession.isAccessTokenExpiring()) {
            return authSession.getAccessToken();
        }
        return refreshOrFail();
    }

    public String tokenOrNull() {
        if (!authSession.isAuthenticated()) {
            return null;
        }
        try {
            return accessToken();
        } catch (PlatformApiException ex) {
            return null;
        }
    }

    private String refreshOrFail() {
        String refreshToken = authSession.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            invalidate();
            throw PlatformApiException.of("UNAUTHORIZED", "Sesion expirada", null);
        }
        try {
            TokenResponse response = authRestClient.refresh(refreshToken, correlationId());
            authSession.refreshAccessToken(response);
            return authSession.getAccessToken();
        } catch (PlatformApiException ex) {
            log.info("Refresh token rechazado, se cierra la sesion: {}", ex.getCode());
            invalidate();
            throw PlatformApiException.of("UNAUTHORIZED", "Sesion expirada, vuelva a iniciar sesion", null);
        }
    }

    public void invalidate() {
        authSession.clear();
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            var session = servletAttributes.getRequest().getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }

    public static String correlationId() {
        return UUID.randomUUID().toString();
    }
}
