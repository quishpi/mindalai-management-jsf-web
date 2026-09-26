package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.AuthRestClient;
import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.dto.TokenResponse;
import ec.mindalai.managementjsf.security.AuthSession;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import lombok.Setter;


import java.io.Serializable;

/** Vista de acceso al backoffice de plataforma. */
@Slf4j
@Named("loginBean")
@ViewScoped
@Getter
@Setter
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AuthRestClient authRestClient;

    @Inject
    private AuthSession authSession;

    private String username;
    private String password;
    private boolean rememberUsername = true;

    public String login() {
        if (isBlank(username) || isBlank(password)) {
            addError("Ingrese usuario y contrasena");
            return null;
        }
        try {
            TokenResponse response = authRestClient.login(username.trim(), password, AuthRestClient.newCorrelationId());
            authSession.start(response);
            password = null;
            return "/dashboard.xhtml?faces-redirect=true";
        } catch (PlatformApiException ex) {
            password = null;
            addError("INVALID_CREDENTIALS".equals(ex.getCode())
                    ? "Usuario o contrasena invalidos"
                    : ex.getMessage());
            return null;
        }
    }

    /** Cierra la sesion local y vuelve al login. El token del API solo vive en memoria. */
    public String logout() {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().invalidateSession();
        try {
            context.getExternalContext().redirect("/login.xhtml?faces-redirect=true");
        } catch (java.io.IOException ex) {
            log.warn("No se pudo redirigir al login tras cerrar sesion: {}", ex.getMessage());
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void addError(String text) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
    }
}
