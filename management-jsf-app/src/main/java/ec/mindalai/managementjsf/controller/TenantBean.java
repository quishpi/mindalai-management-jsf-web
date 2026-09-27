package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.TenantDto;
import ec.mindalai.managementjsf.dto.TenantStatusHistoryDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tenants: alta, edicion y ciclo de vida comercial. Las suspensiones y reactivaciones exigen un
 * motivo que queda registrado en el historial de estados del tenant junto con su fecha y hora.
 */
@Named("tenantBean")
@ViewScoped
@Getter
@Setter
public class TenantBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private static final String ACTION_SUSPEND = "SUSPEND";
    private static final String ACTION_REACTIVATE = "REACTIVATE";

    private static final String TENANT_DIALOG = "tenantDialogWidget";
    private static final String STATUS_DIALOG = "statusDialogWidget";
    private static final String HISTORY_DIALOG = "historyDialogWidget";

    private static final String[] STATUSES = {"ACTIVE", "SUSPENDED", "INACTIVE"};

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

    /** Tenant sobre el que se confirmo el cambio de estado o se pidio el historial. */
    private TenantDto selectedTenant;
    /** {@code SUSPEND} o {@code REACTIVATE} mientras el dialog de estado esta abierto. */
    private String statusAction;
    private String statusReason;

    /** Historial ya consultado, por tenant, para no repetir la llamada al abrir el dialog. */
    private final Map<UUID, List<TenantStatusHistoryDto>> historyByTenant = new HashMap<>();
    private List<TenantStatusHistoryDto> selectedHistory = new ArrayList<>();

    public String[] getStatuses() {
        return STATUSES.clone();
    }

    /**
     * Numero de orden de una fila. {@code rowIndexVar} de la tabla entrega el indice global de
     * la fila, de modo que la numeracion continua entre paginas sin depender del bean.
     */
    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        tenants = call(() -> restClient.tenants(token, cid, blankToNull(statusFilter), blankToNull(search), null),
                tenants);
    }

    /** Prepara el dialog de alta: el RUC solo se puede fijar en la creacion. */
    public void openNew() {
        resetForm();
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
        if (updated == null) {
            return;
        }
        success("Tenant " + updated.getRuc() + " actualizado");
        replaceInPlace(updated);
        resetForm();
        hideDialog(TENANT_DIALOG);
    }

    public void requestSuspend(TenantDto tenant) {
        prepareStatusChange(tenant, ACTION_SUSPEND);
    }

    public void requestReactivate(TenantDto tenant) {
        prepareStatusChange(tenant, ACTION_REACTIVATE);
    }

    public void confirmStatusChange() {
        if (selectedTenant == null || statusAction == null) {
            error("Seleccione el tenant al que aplicar el cambio de estado");
            return;
        }
        String reason = blankToNull(statusReason);
        if (reason == null) {
            error("Indique el motivo del cambio de estado");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = selectedTenant.getId();
        boolean suspending = ACTION_SUSPEND.equals(statusAction);
        TenantDto updated = call(() -> suspending
                ? restClient.suspendTenant(token, cid, id, reason)
                : restClient.reactivateTenant(token, cid, id, reason), null);
        if (updated == null) {
            return;
        }
        success("Tenant " + updated.getRuc() + (suspending ? " suspendido" : " reactivado"));
        replaceInPlace(updated);
        refreshHistory(updated);
        statusAction = null;
        statusReason = null;
        hideDialog(STATUS_DIALOG);
        showDialog(HISTORY_DIALOG);
    }

    /** Carga el historial del tenant y deja el dialog listo para mostrarlo. */
    public void openHistory(TenantDto tenant) {
        if (tenant == null) {
            error("Seleccione un tenant");
            return;
        }
        selectedTenant = tenant;
        refreshHistory(tenant);
    }

    /** Severidad del tag de estado: SUSPENDED se muestra sobre fondo rojo. */
    public String statusSeverity(String status) {
        if (STATUS_ACTIVE.equals(status)) {
            return "success";
        }
        return STATUS_SUSPENDED.equals(status) ? "danger" : "secondary";
    }

    private void create() {
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
                null, this::duplicateRucMessage);
        if (created == null) {
            return;
        }
        success("Tenant " + created.getRuc() + " registrado");
        resetForm();
        hideDialog(TENANT_DIALOG);
        load();
    }

    private String duplicateRucMessage(PlatformApiException ex) {
        if ("TENANT_DUPLICATED_RUC".equals(ex.getError().detailText())) {
            return "El RUC " + ruc.trim() + " ya esta registrado en la plataforma. "
                    + "Verifique el RUC o edite el tenant existente.";
        }
        return ex.getMessage();
    }

    private void prepareStatusChange(TenantDto tenant, String action) {
        if (tenant == null) {
            error("Seleccione un tenant");
            return;
        }
        selectedTenant = tenant;
        statusAction = action;
        statusReason = null;
    }

    private void refreshHistory(TenantDto tenant) {
        List<TenantStatusHistoryDto> entries = call(
                () -> restClient.tenantStatusHistory(token(), correlationId(), tenant.getId()), null);
        List<TenantStatusHistoryDto> safe = entries == null ? List.of() : entries;
        historyByTenant.put(tenant.getId(), safe);
        if (selectedTenant != null && selectedTenant.getId().equals(tenant.getId())) {
            selectedHistory = safe;
        }
    }

    /**
     * Sustituye la fila editada por la version devuelta por el API. Evita recargar la lista
     * completa, que reordenaria la tabla (el backend la ordena por razon social).
     */
    private void replaceInPlace(TenantDto updated) {
        for (int i = 0; i < tenants.size(); i++) {
            if (updated.getId().equals(tenants.get(i).getId())) {
                tenants.set(i, updated);
                return;
            }
        }
        tenants.add(0, updated);
    }

    private void hideDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }

    private void showDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').show();");
    }

    private void resetForm() {
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
