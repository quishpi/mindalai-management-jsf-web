package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.security.AuthSession;
import ec.mindalai.managementjsf.security.PlatformSecurity;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base comun de las vistas de plataforma: token vigente, correlation id por peticion y
 * traduccion de {@link PlatformApiException} a mensaje de UI. Un 401 cierra la sesion y
 * devuelve al login en lugar de dejar la vista a medias.
 */
@Slf4j
public abstract class AbstractPageBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    protected PlatformSecurity platformSecurity;

    @Inject
    protected AuthSession authSession;

    protected String correlationId() {
        return UUID.randomUUID().toString();
    }

    protected String token() {
        return platformSecurity.accessToken();
    }

    /** Ejecuta una llamada al API y devuelve {@code fallback} ante error, notificando al operador. */
    protected <T> T call(Supplier<T> operation, T fallback) {
        try {
            return operation.get();
        } catch (PlatformApiException ex) {
            handle(ex);
            return fallback;
        }
    }

    /**
     * Igual que {@link #call(Supplier, Object)} pero permite reformular el mensaje de los conflictos
     * (HTTP 409), que el backend expresa con un codigo de detalle y un texto pensados para el API.
     * Cualquier otro error sigue el camino comun, incluida la expiracion de sesion.
     */
    protected <T> T call(Supplier<T> operation, T fallback, Function<PlatformApiException, String> conflictMessage) {
        try {
            return operation.get();
        } catch (PlatformApiException ex) {
            if (ex.isConflict()) {
                addMessage(FacesMessage.SEVERITY_ERROR, conflictMessage.apply(ex));
                return fallback;
            }
            handle(ex);
            return fallback;
        }
    }

    protected void run(Runnable operation) {
        try {
            operation.run();
        } catch (PlatformApiException ex) {
            handle(ex);
        }
    }

    private void handle(PlatformApiException ex) {
        if (ex.isUnauthorized()) {
            platformSecurity.invalidate();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Sesion expirada, vuelva a iniciar sesion", null));
            redirectToLogin();
            return;
        }
        addMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage());
    }

    private void redirectToLogin() {
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect("/login.xhtml?faces-redirect=true");
        } catch (java.io.IOException ex) {
            log.warn("No se pudo redirigir al login: {}", ex.getMessage());
        }
    }

    protected void addMessage(jakarta.faces.application.FacesMessage.Severity severity, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, text, null));
    }

    protected void success(String text) {
        addMessage(FacesMessage.SEVERITY_INFO, text);
    }

    protected void warn(String text) {
        addMessage(FacesMessage.SEVERITY_WARN, text);
    }

    protected void error(String text) {
        addMessage(FacesMessage.SEVERITY_ERROR, text);
    }
}
