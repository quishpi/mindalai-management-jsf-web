package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.ConfigurationDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Configuracion global de la plataforma, agrupada por categoria. */
@Named("configurationBean")
@ViewScoped
@Getter
@Setter
public class ConfigurationBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<ConfigurationDto> entries = new ArrayList<>();
    private String categoryFilter;
    private String editKey;
    private String editValue;

    public void load() {
        String cid = correlationId();
        String token = token();
        entries = call(() -> restClient.configuration(token, cid, categoryFilter), entries);
    }

    public void startEdit(ConfigurationDto entry) {
        editKey = entry.getConfigKey();
        editValue = entry.getConfigValue();
    }

    public void cancelEdit() {
        editKey = null;
        editValue = null;
    }

    public void save() {
        if (editKey == null || editValue == null || editValue.isBlank()) {
            error("El valor no puede quedar vacio");
            return;
        }
        String cid = correlationId();
        String token = token();
        String key = editKey;
        String value = editValue.trim();
        ConfigurationDto updated = call(() -> restClient.updateConfiguration(token, cid, key, value), null);
        if (updated != null) {
            success("Configuracion actualizada: " + key);
            cancelEdit();
            load();
        }
    }
}
