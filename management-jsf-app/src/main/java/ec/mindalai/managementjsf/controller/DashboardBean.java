package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.TenantRestClient;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter; import lombok.RequiredArgsConstructor;
import java.io.Serializable; import java.util.List; import java.util.UUID;

@Named @ViewScoped @RequiredArgsConstructor
public class DashboardBean implements Serializable {
    private final TenantRestClient tenantRestClient;
    @Getter private List<TenantDto> tenants;
    @Getter private String correlationId;
    @PostConstruct public void init(){
        correlationId=UUID.randomUUID().toString();
        String token=(String)FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("authToken");
        if(token==null) token="demo-token-fase1";
        tenants=tenantRestClient.listTenants(token,correlationId);
    }
    public int getTenantCount(){ return tenants!=null?tenants.size():0; }
}
