package ec.mindalai.managementjsf.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Spring Security responde 403 a las peticiones sin sesion, lo que deja al operador en una
 * pagina de error en lugar de devolverlo al login. Como la identidad vive en la HttpSession
 * (ver {@code SecurityConfig}), la sesion caducada o inexistente debe ser un redirect.
 *
 * <p>La navegacion (GET/HEAD de una vista) redirige a la pantalla de acceso. Las peticiones
 * ajax de JSF no pueden seguir un redirect, asi que responden 401 con {@code X-Session-Expired}
 * para que el bean los traduzca a mensaje y redirection.</p>
 */
public class LoginEntryPoint implements AuthenticationEntryPoint {

    private static final String LOGIN = "/login.xhtml?faces-redirect=true";
    private static final String AJAX_HEADER = "Faces-Request";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (isAjax(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("X-Session-Expired", "true");
            return;
        }
        response.sendRedirect(request.getContextPath() + LOGIN);
    }

    private boolean isAjax(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return false;
        }
        return request.getHeader(AJAX_HEADER) != null
                || "partial/ajax".equals(request.getHeader("Faces-Request"))
                || request.getHeader("X-Requested-With") != null;
    }
}
