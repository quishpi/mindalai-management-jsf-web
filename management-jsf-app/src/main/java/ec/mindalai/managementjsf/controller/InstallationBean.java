package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.InstallationDto;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Instalaciones: puntos de operacion de cada tenant. */
@Named("installationBean")
@ViewScoped
@Getter
@Setter
public class InstallationBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<InstallationDto> installations = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();

    private UUID tenantFilter;
    private UUID tenantId;
    private String name;
    private String address;
    private String contactName;

    public void load() {
        String cid = correlationId();
        String token = token();
        installations = call(() -> restClient.installations(token, cid, tenantFilter), installations);
        tenants = call(() -> restClient.tenants(token, cid, "ACTIVE", null, null), tenants);
    }

    public void create() {
        if (tenantId == null || name == null || name.isBlank()) {
            error("Seleccione tenant e indique el nombre de la instalacion");
            return;
        }
        String cid = correlationId();
        String token = token();
        String installationName = name.trim();
        String installationAddress = blankToNull(address);
        String contact = blankToNull(contactName);
        InstallationDto created = call(
                () -> restClient.createInstallation(token, cid, tenantId, installationName, installationAddress, contact),
                null);
        if (created != null) {
            success("Instalacion registrada");
            name = null;
            address = null;
            contactName = null;
            load();
        }
    }

    public void toggleMaintenance(InstallationDto installation, boolean maintenance) {
        run(() -> {
            restClient.setInstallationMaintenance(token(), correlationId(), installation.getId(), maintenance);
            success(maintenance ? "Instalacion en mantenimiento" : "Instalacion fuera de mantenimiento");
            load();
        });
    }

    public void deactivate(InstallationDto installation) {
        run(() -> {
            restClient.deactivateInstallation(token(), correlationId(), installation.getId());
            success("Instalacion desactivada");
            load();
        });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
