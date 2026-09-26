package ec.mindalai.managementjsf.controller;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;

/** Raiz del backoffice: resuelve al login o al dashboard segun la sesion. */
@Named("indexBean")
@ViewScoped
public class IndexBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ec.mindalai.managementjsf.security.AuthSession authSession;

    public IndexBean(ec.mindalai.managementjsf.security.AuthSession authSession) {
        this.authSession = authSession;
    }

    public String resolve() {
        return authSession.isAuthenticated()
                ? "/dashboard.xhtml?faces-redirect=true"
                : "/login.xhtml?faces-redirect=true";
    }
}
