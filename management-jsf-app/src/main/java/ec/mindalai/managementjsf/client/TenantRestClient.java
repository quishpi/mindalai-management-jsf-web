package ec.mindalai.managementjsf.client;

import ec.mindalai.managementjsf.dto.TenantDto;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class TenantRestClient {
    private final RestClient restClient;
    public List<TenantDto> listTenants(String token, String cid){
        try{
            return restClient.get().uri("/api/v1.0/platform/tenants").header("Authorization","Bearer "+token).header("X-Correlation-Id",cid).retrieve().body(new ParameterizedTypeReference<List<TenantDto>>() {});
        }catch(Exception e){ log.warn("Failed tenants",e); return List.of(); }
    }
    public TenantDto createTenant(TenantDto dto, String token, String cid){
        return restClient.post().uri("/api/v1.0/platform/tenants").header("Authorization","Bearer "+token).header("X-Correlation-Id",cid).body(dto).retrieve().body(TenantDto.class);
    }
}
