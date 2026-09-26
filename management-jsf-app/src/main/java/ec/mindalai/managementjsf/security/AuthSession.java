package ec.mindalai.managementjsf.security;

import ec.mindalai.managementjsf.dto.TokenResponse;
import ec.mindalai.managementjsf.dto.UserProfileDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Sesion del operador de plataforma. El token vive en la HttpSession (nunca en la vista)
 * y se renueva de forma transparente a traves de {@code POST /auth/refresh} cuando expira.
 */
@Slf4j
@Component
@SessionScope
@Getter
@Setter
public class AuthSession implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Set<String> PLATFORM_ROLES =
            Set.of("SUPER_ADMIN", "PLATFORM_ADMIN", "PLATFORM_OPERATOR", "PLATFORM_AUDITOR");

    private String accessToken;
    private String refreshToken;
    private Instant accessTokenExpiresAt;
    private String username;
    private String fullName;
    private String email;
    private List<String> roles = List.of();

    public void start(TokenResponse response) {
        this.accessToken = response.getAccessToken();
        this.refreshToken = response.getRefreshToken();
        this.accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(response.getExpiresIn(), 0));
        applyProfile(response.getUser());
    }

    public void refreshAccessToken(TokenResponse response) {
        this.accessToken = response.getAccessToken();
        if (response.getRefreshToken() != null && !response.getRefreshToken().isBlank()) {
            this.refreshToken = response.getRefreshToken();
        }
        this.accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(response.getExpiresIn(), 0));
        if (response.getUser() != null) {
            applyProfile(response.getUser());
        }
    }

    private void applyProfile(UserProfileDto profile) {
        if (profile == null) {
            return;
        }
        this.username = profile.getUsername();
        this.fullName = profile.getFullName() != null ? profile.getFullName() : profile.getUsername();
        this.email = profile.getEmail();
        this.roles = profile.getRoles() == null ? List.of() : List.copyOf(profile.getRoles());
    }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank();
    }

    /** Margen de 30s para renovar antes del vencimiento real. */
    public boolean isAccessTokenExpiring() {
        return accessTokenExpiresAt == null
                || accessTokenExpiresAt.isBefore(Instant.now().plusSeconds(30));
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /** Un usuario de plataforma siempre tiene alguno de los roles sembrados en V2. */
    public boolean isPlatformOperator() {
        return roles != null && roles.stream().anyMatch(PLATFORM_ROLES::contains);
    }

    /** PLATFORM_AUDITOR y PLATFORM_OPERATOR no agregan master data ni operacion comercial. */
    public boolean canWriteCommercial() {
        return hasRole("SUPER_ADMIN") || hasRole("PLATFORM_ADMIN");
    }

    public boolean canProvision() {
        return canWriteCommercial() || hasRole("PLATFORM_OPERATOR");
    }

    public boolean canSupport() {
        return canWriteCommercial() || hasRole("PLATFORM_OPERATOR");
    }

    public void clear() {
        this.accessToken = null;
        this.refreshToken = null;
        this.accessTokenExpiresAt = null;
        this.username = null;
        this.fullName = null;
        this.email = null;
        this.roles = List.of();
    }

    public String displayName() {
        return fullName != null && !fullName.isBlank() ? fullName : username;
    }

    public String roleLabel() {
        if (roles == null || roles.isEmpty()) {
            return "-";
        }
        return String.join(", ", roles);
    }
}
