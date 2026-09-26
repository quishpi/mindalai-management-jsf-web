package ec.mindalai.managementjsf.client;

import ec.mindalai.managementjsf.dto.TokenResponse;
import ec.mindalai.managementjsf.dto.UserProfileDto;
import ec.mindalai.managementjsf.security.AuthSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Cliente de autenticacion contra el API de plataforma. Es el unico punto que conoce
 * el par de tokens; el resto de recursos solo pide el access token vigente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRestClient {

    private final PlatformApiClient api;

    public TokenResponse login(String username, String password, String correlationId) {
        return api.post(PlatformApiClient.platformPath("/auth/login"), null, correlationId,
                PlatformApiClient.body("username", username, "password", password), TokenResponse.class);
    }

    public TokenResponse refresh(String refreshToken, String correlationId) {
        return api.post(PlatformApiClient.platformPath("/auth/refresh"), null, correlationId,
                PlatformApiClient.body("refreshToken", refreshToken), TokenResponse.class);
    }

    public UserProfileDto me(String accessToken, String correlationId) {
        return api.get(PlatformApiClient.platformPath("/auth/me"), accessToken, correlationId, UserProfileDto.class);
    }

    public void changePassword(String accessToken, String currentPassword, String newPassword, String correlationId) {
        api.post(PlatformApiClient.platformPath("/auth/change-password"), accessToken, correlationId,
                PlatformApiClient.body("currentPassword", currentPassword, "newPassword", newPassword),
                Void.class);
    }

    public static String newCorrelationId() {
        return UUID.randomUUID().toString();
    }

    static Map<String, Object> emptyQuery() {
        return Map.of();
    }
}
