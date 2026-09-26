package ec.mindalai.managementjsf.security;

import ec.mindalai.managementjsf.dto.TokenResponse;
import ec.mindalai.managementjsf.dto.UserProfileDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthSessionTest {

    private static TokenResponse response(List<String> roles, long expiresIn) {
        return TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .refreshExpiresIn(3600)
                .user(UserProfileDto.builder()
                        .username("admin")
                        .fullName("Administrador de Plataforma")
                        .email("admin@mindalai.local")
                        .scope("platform")
                        .roles(roles)
                        .build())
                .build();
    }

    @Test
    void startStoresTokensAndProfile() {
        AuthSession session = new AuthSession();

        session.start(response(List.of("SUPER_ADMIN"), 900));

        assertThat(session.isAuthenticated()).isTrue();
        assertThat(session.getAccessToken()).isEqualTo("access-token");
        assertThat(session.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(session.getUsername()).isEqualTo("admin");
        assertThat(session.displayName()).isEqualTo("Administrador de Plataforma");
        assertThat(session.hasRole("SUPER_ADMIN")).isTrue();
    }

    @Test
    void accessTokenNotExpiringWhenValid() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("PLATFORM_ADMIN"), 900));

        assertThat(session.isAccessTokenExpiring()).isFalse();
    }

    @Test
    void accessTokenExpiringWithoutExpiry() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("PLATFORM_ADMIN"), 0));

        assertThat(session.isAccessTokenExpiring()).isTrue();
    }

    @Test
    void auditorCannotWriteCommercialData() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("PLATFORM_AUDITOR"), 900));

        assertThat(session.isPlatformOperator()).isTrue();
        assertThat(session.canWriteCommercial()).isFalse();
        assertThat(session.canProvision()).isFalse();
        assertThat(session.canSupport()).isFalse();
    }

    @Test
    void platformAdminCanWriteAndProvision() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("PLATFORM_ADMIN"), 900));

        assertThat(session.canWriteCommercial()).isTrue();
        assertThat(session.canProvision()).isTrue();
        assertThat(session.canSupport()).isTrue();
    }

    @Test
    void operatorProvisionsAndSupportsButDoesNotWriteCommercial() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("PLATFORM_OPERATOR"), 900));

        assertThat(session.canWriteCommercial()).isFalse();
        assertThat(session.canProvision()).isTrue();
        assertThat(session.canSupport()).isTrue();
    }

    @Test
    void clearRemovesEveryTraceOfTheSession() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("SUPER_ADMIN"), 900));

        session.clear();

        assertThat(session.isAuthenticated()).isFalse();
        assertThat(session.getAccessToken()).isNull();
        assertThat(session.getRefreshToken()).isNull();
        assertThat(session.getUsername()).isNull();
        assertThat(session.getRoles()).isEmpty();
    }

    @Test
    void refreshKeepsPreviousRefreshTokenWhenApiDoesNotRotateIt() {
        AuthSession session = new AuthSession();
        session.start(response(List.of("PLATFORM_ADMIN"), 900));

        session.refreshAccessToken(TokenResponse.builder()
                .accessToken("new-access")
                .refreshToken(null)
                .expiresIn(900)
                .build());

        assertThat(session.getAccessToken()).isEqualTo("new-access");
        assertThat(session.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(session.getUsername()).isEqualTo("admin");
    }
}
