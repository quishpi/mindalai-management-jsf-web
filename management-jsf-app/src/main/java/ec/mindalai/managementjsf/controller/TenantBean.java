package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.TenantRestClient;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter; import lombok.Setter; import lombok.RequiredArgsConstructor;
import java.io.Serializable; import java.util.List; import java.util.UUID;

@Named @ViewScoped @RequiredArgsConstructor
public class TenantBean implements Serializable {
    private final TenantRestClient tenantRestClient;
    @Getter @Setter private String legalName;
    @Getter @Setter private String ruc;
    @Getter @Setter private String tradeName;
    @Getter private List<TenantDto> tenants;
    public void load(){
        String token=(String)FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("authToken");
        if(token==null) token="demo-token-fase1";
        tenants=tenantRestClient.listTenants(token, UUID.randomUUID().toString());
    }
    public void create(){
        try{
            String token=(String)FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("authToken");
            if(token==null) token="demo-token-fase1";
            TenantDto dto=TenantDto.builder().legalName(legalName).ruc(ruc).tradeName(tradeName).build();
            tenantRestClient.createTenant(dto, token, UUID.randomUUID().toString());
            FacesContext.getCurrentInstance().addMessage(null,new FacesMessage("Tenant creado"));
            load();
        }catch(Exception e){ FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(FacesMessage.SEVERITY_ERROR,"Error: "+e.getMessage(),null)); }
    }
}
