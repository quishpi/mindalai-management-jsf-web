package ec.mindalai.managementjsf.config;

import ec.mindalai.managementjsf.security.AuthSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gate del backoffice. La identidad NO la resuelve Spring Security: la resuelve el API de
 * plataforma y viaja en la HttpSession, asi que este filtro solo proyecta la sesion a un
 * {@code Authentication} de Spring. El token nunca se acepta desde el navegador.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthSession authSession;

    private static final PathPatternRequestMatcher.Builder MATCHER = PathPatternRequestMatcher.withDefaults();

    private static final RequestMatcher PUBLIC = RequestMatchers.anyOf(
            MATCHER.matcher("/login.xhtml"),
            MATCHER.matcher("/"),
            MATCHER.matcher("/index.xhtml"),
            MATCHER.matcher("/javax.faces.resource/**"),
            MATCHER.matcher("/jakarta.faces.resource/**"),
            MATCHER.matcher("/resources/**"),
            MATCHER.matcher("/favicon.ico"),
            MATCHER.matcher("/error"),
            MATCHER.matcher(HttpMethod.GET, "/actuator/health"),
            MATCHER.matcher(HttpMethod.GET, "/actuator/info"));

    /**
     * Los POST de JSF se protegen con el ViewState de Facelets, no con el token CSRF de Spring
     * (que un formulario JSF no puede portar). El token CSRF queda activo para el resto.
     */
    private static final RequestMatcher CSRF_EXEMPT = RequestMatchers.anyOf(
            MATCHER.matcher(HttpMethod.POST, "/*.xhtml"),
            MATCHER.matcher(HttpMethod.POST, "/javax.faces.resource/**"),
            MATCHER.matcher(HttpMethod.POST, "/jakarta.faces.resource/**"));

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(CSRF_EXEMPT))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.xhtml?faces-redirect=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .addFilterBefore(sessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(Customizer.withDefaults()));
        return http.build();
    }

    private OncePerRequestFilter sessionAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                populateSecurityContext();
                chain.doFilter(request, response);
            }
        };
    }

    private void populateSecurityContext() {
        SecurityContextHolder.clearContext();
        if (!authSession.isAuthenticated() || authSession.getRoles() == null) {
            return;
        }
        List<SimpleGrantedAuthority> authorities = authSession.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        if (authorities.isEmpty()) {
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                authSession.getUsername(), null, authorities);
        authentication.setDetails(authSession.displayName());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
