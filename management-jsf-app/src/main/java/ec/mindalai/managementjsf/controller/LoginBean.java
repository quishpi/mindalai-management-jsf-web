package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.AuthRestClient;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter; import lombok.Setter; import lombok.RequiredArgsConstructor;
import java.io.Serializable;

@Named @ViewScoped @RequiredArgsConstructor
public class LoginBean implements Serializable {
    private final AuthRestClient authRestClient;
    @Getter @Setter private String username; @Getter @Setter private String password;
    public String login(){
        String token=authRestClient.login(username,password);
        if(token!=null){ FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("authToken",token); return "/dashboard.xhtml?faces-redirect=true"; }
        FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,"Credenciales inválidas",null)); return null;
    }
    public String logout(){ FacesContext.getCurrentInstance().getExternalContext().invalidateSession(); return "/login.xhtml?faces-redirect=true"; }
}
