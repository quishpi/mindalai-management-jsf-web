package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.InstallationDto;
import ec.mindalai.managementjsf.dto.InstallationStatusHistoryDto;
import ec.mindalai.managementjsf.dto.TenantDto;
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
 * Instalaciones: puntos de operacion de cada tenant. El nombre es unico dentro del tenant.
 * Mantenimiento, activacion y desactivacion exigen un motivo, que queda registrado en el
 * historial de estados de la instalacion.
 */
@Named("installationBean")
@ViewScoped
@Getter
@Setter
public class InstallationBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_MAINTENANCE = "MAINTENANCE";

    private static final String ACTION_ACTIVATE = "ACTIVATE";
    private static final String ACTION_MAINTENANCE = "MAINTENANCE";
    private static final String ACTION_RESUME = "RESUME";
    private static final String ACTION_DEACTIVATE = "DEACTIVATE";

    private static final String INSTALLATION_DIALOG = "installationDialogWidget";
    private static final String STATUS_DIALOG = "installationStatusDialogWidget";
    private static final String HISTORY_DIALOG = "installationHistoryDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<InstallationDto> installations = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private UUID tenantFilter;
    private String statusFilter;

    private UUID tenantId;
    private String name;
    private String address;
    private String contactName;

    private UUID editingId;
    private boolean editing;

    /** Instalacion sobre la que se confirmo el cambio de estado o se pidio el historial. */
    private InstallationDto selected;
    private String statusAction;
    private String statusReason;

    /** Historial ya consultado por instalacion, para no repetir la llamada al abrir el dialog. */
    private final Map<UUID, List<InstallationStatusHistoryDto>> historyByInstallation = new HashMap<>();
    private List<InstallationStatusHistoryDto> selectedHistory = new ArrayList<>();

    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    /**
     * El API no expone un filtro de estado para instalaciones, asi que el recorte es de vista:
     * se aplica sobre la lista ya cargada y por eso tambien es el contenido que se exporta.
     */
    public List<InstallationDto> visibleInstallations() {
        if (statusFilter == null || statusFilter.isBlank()) {
            return installations;
        }
        return installations.stream()
                .filter(installation -> statusFilter.equals(installation.getStatus()))
                .toList();
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        installations = call(() -> restClient.installations(token, cid, tenantFilter), installations);
        tenants = call(() -> restClient.tenants(token, cid, "ACTIVE", null, null), tenants);
    }

    public void openNew() {
        resetForm();
    }

    public void edit(InstallationDto installation) {
        editing = true;
        editingId = installation.getId();
        tenantId = installation.getTenantId();
        name = installation.getName();
        address = installation.getAddress();
        contactName = installation.getContactName();
    }

    public void save() {
        if (editingId == null) {
            create();
            return;
        }
        if (isBlank(name)) {
            error("El nombre de la instalacion es obligatorio");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = editingId;
        String installationName = name.trim();
        String installationAddress = blankToNull(address);
        String contact = blankToNull(contactName);
        InstallationDto updated = call(
                () -> restClient.updateInstallation(token, cid, id, installationName, installationAddress, contact),
                null, this::duplicatedNameMessage);
        if (updated == null) {
            return;
        }
        success("Instalacion " + updated.getName() + " actualizada");
        replaceInPlace(updated);
        resetForm();
        hideDialog(INSTALLATION_DIALOG);
    }

    public void requestActivate(InstallationDto installation) {
        prepareStatusChange(installation, ACTION_ACTIVATE);
    }

    public void requestMaintenance(InstallationDto installation) {
        prepareStatusChange(installation, ACTION_MAINTENANCE);
    }

    public void requestResume(InstallationDto installation) {
        prepareStatusChange(installation, ACTION_RESUME);
    }

    public void requestDeactivate(InstallationDto installation) {
        prepareStatusChange(installation, ACTION_DEACTIVATE);
    }

    public void confirmStatusChange() {
        if (selected == null || statusAction == null) {
            error("Seleccione la instalacion a la que aplicar el cambio de estado");
            return;
        }
        String reason = blankToNull(statusReason);
        if (reason == null) {
            error("Indique el motivo del cambio de estado");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = selected.getId();
        String action = statusAction;
        InstallationDto updated = call(() -> {
            if (ACTION_ACTIVATE.equals(action)) {
                return restClient.activateInstallation(token, cid, id, reason);
            }
            if (ACTION_MAINTENANCE.equals(action)) {
                return restClient.setInstallationMaintenance(token, cid, id, true, reason);
            }
            if (ACTION_RESUME.equals(action)) {
                return restClient.setInstallationMaintenance(token, cid, id, false, reason);
            }
            return restClient.deactivateInstallation(token, cid, id, reason);
        }, null, this::invalidTransitionMessage);
        if (updated == null) {
            return;
        }
        success(successMessage(action, updated));
        replaceInPlace(updated);
        refreshHistory(updated);
        statusAction = null;
        statusReason = null;
        hideDialog(STATUS_DIALOG);
        showDialog(HISTORY_DIALOG);
    }

    public void openHistory(InstallationDto installation) {
        if (installation == null) {
            error("Seleccione una instalacion");
            return;
        }
        selected = installation;
        refreshHistory(installation);
    }

    public String statusSeverity(String status) {
        if (STATUS_ACTIVE.equals(status)) {
            return "success";
        }
        return STATUS_MAINTENANCE.equals(status) ? "warning" : "secondary";
    }

    /** Texto del boton de confirmacion del dialog, segun la transicion solicitada. */
    public String statusActionLabel() {
        if (ACTION_ACTIVATE.equals(statusAction)) {
            return "Activar";
        }
        if (ACTION_MAINTENANCE.equals(statusAction)) {
            return "Poner en mantenimiento";
        }
        if (ACTION_RESUME.equals(statusAction)) {
            return "Reanudar";
        }
        return "Desactivar";
    }

    private String successMessage(String action, InstallationDto updated) {
        if (ACTION_ACTIVATE.equals(action)) {
            return "Instalacion " + updated.getName() + " activada";
        }
        if (ACTION_MAINTENANCE.equals(action)) {
            return "Instalacion " + updated.getName() + " en mantenimiento";
        }
        if (ACTION_RESUME.equals(action)) {
            return "Instalacion " + updated.getName() + " fuera de mantenimiento";
        }
        return "Instalacion " + updated.getName() + " desactivada";
    }

    private void create() {
        if (tenantId == null || isBlank(name)) {
            error("Seleccione tenant e indique el nombre de la instalacion");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID tenant = tenantId;
        String installationName = name.trim();
        String installationAddress = blankToNull(address);
        String contact = blankToNull(contactName);
        InstallationDto created = call(
                () -> restClient.createInstallation(token, cid, tenant, installationName, installationAddress, contact),
                null, this::duplicatedNameMessage);
        if (created == null) {
            return;
        }
        success("Instalacion " + created.getName() + " registrada");
        resetForm();
        hideDialog(INSTALLATION_DIALOG);
        load();
    }

    private String duplicatedNameMessage(PlatformApiException ex) {
        if ("INSTALLATION_DUPLICATED_NAME".equals(ex.getError().detailText())) {
            return "El tenant ya tiene una instalacion con el nombre " + name.trim()
                    + ". Use otro nombre o edite la instalacion existente.";
        }
        return ex.getMessage();
    }

    private String invalidTransitionMessage(PlatformApiException ex) {
        String detail = ex.getError().detailText();
        if (detail != null && detail.contains("INVALID_TRANSITION")) {
            return "La instalacion ya esta en el estado destino: recargue la lista y vuelva a intentar.";
        }
        return ex.getMessage();
    }

    private void prepareStatusChange(InstallationDto installation, String action) {
        if (installation == null) {
            error("Seleccione una instalacion");
            return;
        }
        selected = installation;
        statusAction = action;
        statusReason = null;
    }

    private void refreshHistory(InstallationDto installation) {
        List<InstallationStatusHistoryDto> entries = call(
                () -> restClient.installationStatusHistory(token(), correlationId(), installation.getId()), null);
        List<InstallationStatusHistoryDto> safe = entries == null ? List.of() : entries;
        historyByInstallation.put(installation.getId(), safe);
        if (selected != null && selected.getId().equals(installation.getId())) {
            selectedHistory = safe;
        }
    }

    /**
     * Sustituye la fila editada por la version devuelta por el API. Evita recargar la lista
     * completa, que reordenaria la tabla.
     */
    private void replaceInPlace(InstallationDto updated) {
        for (int i = 0; i < installations.size(); i++) {
            if (updated.getId().equals(installations.get(i).getId())) {
                installations.set(i, updated);
                return;
            }
        }
        installations.add(0, updated);
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
        tenantId = null;
        name = null;
        address = null;
        contactName = null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
