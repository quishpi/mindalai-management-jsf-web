package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.DeviceDto;
import ec.mindalai.managementjsf.dto.InstallationDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Dispositivos: servidores y cajas registradas en cada instalacion. */
@Named("deviceBean")
@ViewScoped
@Getter
@Setter
public class DeviceBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String[] DEVICE_TYPES = {"SERVER", "BOX", "PRINTER", "SCANNER", "OTHER"};

    @Inject
    private PlatformRestClient restClient;

    private List<DeviceDto> devices = new ArrayList<>();
    private List<InstallationDto> installations = new ArrayList<>();

    private UUID installationFilter;
    private UUID installationId;
    private String deviceType = "BOX";
    private String identifier;
    private String name;

    public String[] deviceTypes() {
        return DEVICE_TYPES.clone();
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        devices = call(() -> restClient.devices(token, cid, installationFilter), devices);
        installations = call(() -> restClient.installations(token, cid, null), installations);
    }

    public void register() {
        if (installationId == null || identifier == null || identifier.isBlank()) {
            error("Seleccione instalacion e indique el identificador del dispositivo");
            return;
        }
        String cid = correlationId();
        String token = token();
        String type = deviceType == null || deviceType.isBlank() ? "OTHER" : deviceType;
        String deviceIdentifier = identifier.trim();
        String deviceName = blankToNull(name);
        DeviceDto created = call(
                () -> restClient.registerDevice(token, cid, installationId, type, deviceIdentifier, deviceName), null);
        if (created != null) {
            success("Dispositivo registrado");
            identifier = null;
            name = null;
            load();
        }
    }

    public void block(DeviceDto device) {
        changeStatus(device, () -> restClient.blockDevice(token(), correlationId(), device.getId()),
                "Dispositivo bloqueado");
    }

    public void activate(DeviceDto device) {
        changeStatus(device, () -> restClient.activateDevice(token(), correlationId(), device.getId()),
                "Dispositivo activado");
    }

    public void retire(DeviceDto device) {
        changeStatus(device, () -> restClient.retireDevice(token(), correlationId(), device.getId()),
                "Dispositivo retirado");
    }

    public void heartbeat(DeviceDto device) {
        changeStatus(device, () -> restClient.deviceHeartbeat(token(), correlationId(), device.getId()),
                "Heartbeat registrado");
    }

    private void changeStatus(DeviceDto device, Runnable operation, String message) {
        run(() -> {
            operation.run();
            success(message);
            load();
        });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
