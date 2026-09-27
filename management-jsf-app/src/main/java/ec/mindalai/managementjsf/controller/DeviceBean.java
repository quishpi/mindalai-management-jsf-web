package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.DeviceDto;
import ec.mindalai.managementjsf.dto.DeviceStatusHistoryDto;
import ec.mindalai.managementjsf.dto.InstallationDto;
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
 * Dispositivos: servidores y cajas registradas en cada instalacion. El identificador es unico
 * en la plataforma y cada cambio de estado (bloqueo, activacion o retiro) exige un motivo, que
 * queda registrado en el historial de estados del dispositivo.
 */
@Named("deviceBean")
@ViewScoped
@Getter
@Setter
public class DeviceBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String[] DEVICE_TYPES = {"SERVER", "BOX", "PRINTER", "SCANNER", "OTHER"};

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_BLOCKED = "BLOCKED";

    private static final String ACTION_BLOCK = "BLOCK";
    private static final String ACTION_ACTIVATE = "ACTIVATE";
    private static final String ACTION_RETIRE = "RETIRE";

    private static final String DEVICE_DIALOG = "deviceDialogWidget";
    private static final String STATUS_DIALOG = "deviceStatusDialogWidget";
    private static final String HISTORY_DIALOG = "deviceHistoryDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<DeviceDto> devices = new ArrayList<>();
    private List<InstallationDto> installations = new ArrayList<>();
    private UUID installationFilter;

    private UUID installationId;
    private String deviceType = "BOX";
    private String identifier;
    private String name;

    /** Dispositivo sobre el que se confirmo el cambio de estado o se pidio el historial. */
    private DeviceDto selected;
    private String statusAction;
    private String statusReason;

    private final Map<UUID, List<DeviceStatusHistoryDto>> historyByDevice = new HashMap<>();
    private List<DeviceStatusHistoryDto> selectedHistory = new ArrayList<>();

    public String[] deviceTypes() {
        return DEVICE_TYPES.clone();
    }

    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        devices = call(() -> restClient.devices(token, cid, installationFilter), devices);
        installations = call(() -> restClient.installations(token, cid, null), installations);
    }

    public void openNew() {
        installationId = installationFilter;
        deviceType = "BOX";
        identifier = null;
        name = null;
    }

    public void save() {
        if (installationId == null || isBlank(identifier)) {
            error("Seleccione instalacion e indique el identificador del dispositivo");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID installation = installationId;
        String type = isBlank(deviceType) ? "OTHER" : deviceType;
        String deviceIdentifier = identifier.trim();
        String deviceName = blankToNull(name);
        DeviceDto created = call(
                () -> restClient.registerDevice(token, cid, installation, type, deviceIdentifier, deviceName), null);
        if (created == null) {
            return;
        }
        success("Dispositivo " + deviceIdentifier + " registrado");
        identifier = null;
        name = null;
        hideDialog(DEVICE_DIALOG);
        load();
    }

    public void requestBlock(DeviceDto device) {
        prepareStatusChange(device, ACTION_BLOCK);
    }

    public void requestActivate(DeviceDto device) {
        prepareStatusChange(device, ACTION_ACTIVATE);
    }

    public void requestRetire(DeviceDto device) {
        prepareStatusChange(device, ACTION_RETIRE);
    }

    public void confirmStatusChange() {
        if (selected == null || statusAction == null) {
            error("Seleccione el dispositivo al que aplicar el cambio de estado");
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
        DeviceDto updated = call(() -> {
            if (ACTION_BLOCK.equals(action)) {
                return restClient.blockDevice(token, cid, id, reason);
            }
            if (ACTION_ACTIVATE.equals(action)) {
                return restClient.activateDevice(token, cid, id, reason);
            }
            return restClient.retireDevice(token, cid, id, reason);
        }, null);
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

    public void openHistory(DeviceDto device) {
        if (device == null) {
            error("Seleccione un dispositivo");
            return;
        }
        selected = device;
        refreshHistory(device);
    }

    public void heartbeat(DeviceDto device) {
        String cid = correlationId();
        String token = token();
        UUID id = device.getId();
        DeviceDto updated = call(() -> restClient.deviceHeartbeat(token, cid, id), null);
        if (updated == null) {
            return;
        }
        success("Latido de " + updated.getIdentifier() + " registrado");
        replaceInPlace(updated);
    }

    public String statusSeverity(String status) {
        if (STATUS_ACTIVE.equals(status)) {
            return "success";
        }
        return STATUS_BLOCKED.equals(status) ? "danger" : "secondary";
    }

    public String statusActionLabel() {
        if (ACTION_BLOCK.equals(statusAction)) {
            return "Bloquear";
        }
        if (ACTION_ACTIVATE.equals(statusAction)) {
            return "Activar";
        }
        return "Retirar";
    }

    /** El selector de instalacion solo ofrece instalaciones activas. */
    public List<InstallationDto> activeInstallations() {
        return installations.stream()
                .filter(installation -> STATUS_ACTIVE.equals(installation.getStatus()))
                .toList();
    }

    private String successMessage(String action, DeviceDto updated) {
        if (ACTION_BLOCK.equals(action)) {
            return "Dispositivo " + updated.getIdentifier() + " bloqueado";
        }
        if (ACTION_ACTIVATE.equals(action)) {
            return "Dispositivo " + updated.getIdentifier() + " activado";
        }
        return "Dispositivo " + updated.getIdentifier() + " retirado";
    }

    private void prepareStatusChange(DeviceDto device, String action) {
        if (device == null) {
            error("Seleccione un dispositivo");
            return;
        }
        selected = device;
        statusAction = action;
        statusReason = null;
    }

    private void refreshHistory(DeviceDto device) {
        List<DeviceStatusHistoryDto> entries = call(
                () -> restClient.deviceStatusHistory(token(), correlationId(), device.getId()), null);
        List<DeviceStatusHistoryDto> safe = entries == null ? List.of() : entries;
        historyByDevice.put(device.getId(), safe);
        if (selected != null && selected.getId().equals(device.getId())) {
            selectedHistory = safe;
        }
    }

    private void replaceInPlace(DeviceDto updated) {
        for (int i = 0; i < devices.size(); i++) {
            if (updated.getId().equals(devices.get(i).getId())) {
                devices.set(i, updated);
                return;
            }
        }
        devices.add(0, updated);
    }

    private void hideDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }

    private void showDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').show();");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
