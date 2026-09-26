package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tenants: alta, edicion y ciclo de vida comercial. */
@Named("tenantBean")
@ViewScoped
@Getter
@Setter
public class TenantBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<TenantDto> tenants = new ArrayList<>();
    private String statusFilter;
    private String search;

    private String legalName;
    private String ruc;
    private String tradeName;
    private String email;
    private String phone;

    private UUID editingId;
    private boolean editing;

    private static final String[] STATUSES = {"ACTIVE", "SUSPENDED", "INACTIVE"};

    public String[] getStatuses() {
        return STATUSES.clone();
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        tenants = call(() -> restClient.tenants(token, cid, blankToNull(statusFilter), blankToNull(search), null),
                tenants);
    }

    public void create() {
        if (isBlank(legalName) || isBlank(ruc)) {
            error("Razon social y RUC son obligatorios");
            return;
        }
        String cid = correlationId();
        String token = token();
        String legal = legalName.trim();
        String rucValue = ruc.trim();
        String trade = blankToNull(tradeName);
        String mail = blankToNull(email);
        String phoneValue = blankToNull(phone);
        TenantDto created = call(() -> restClient.createTenant(token, cid, legal, rucValue, trade, mail, phoneValue),
                null);
        if (created != null) {
            success("Tenant " + created.getRuc() + " registrado");
            clearForm();
            load();
        }
    }

    public void edit(TenantDto tenant) {
        editing = true;
        editingId = tenant.getId();
        legalName = tenant.getLegalName();
        ruc = tenant.getRuc();
        tradeName = tenant.getTradeName();
        email = tenant.getEmail();
        phone = tenant.getPhone();
    }

    public void save() {
        if (editingId == null) {
            create();
            return;
        }
        if (isBlank(legalName)) {
            error("La razon social es obligatoria");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = editingId;
        String legal = legalName.trim();
        String trade = blankToNull(tradeName);
        String mail = blankToNull(email);
        String phoneValue = blankToNull(phone);
        TenantDto updated = call(() -> restClient.updateTenant(token, cid, id, legal, trade, mail, phoneValue), null);
        if (updated != null) {
            success("Tenant actualizado");
            cancelEdit();
            load();
        }
    }

    public void suspend(TenantDto tenant) {
        run(() -> {
            restClient.suspendTenant(token(), correlationId(), tenant.getId());
            success("Tenant suspendido");
            load();
        });
    }

    public void reactivate(TenantDto tenant) {
        run(() -> {
            restClient.reactivateTenant(token(), correlationId(), tenant.getId());
            success("Tenant reactivado");
            load();
        });
    }

    public void deactivate(TenantDto tenant) {
        run(() -> {
            restClient.deactivateTenant(token(), correlationId(), tenant.getId());
            success("Tenant desactivado");
            load();
        });
    }

    public void cancelEdit() {
        editing = false;
        editingId = null;
        clearForm();
    }

    private void clearForm() {
        legalName = null;
        ruc = null;
        tradeName = null;
        email = null;
        phone = null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
