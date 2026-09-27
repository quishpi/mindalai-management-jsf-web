package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.ConfigurationDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.util.ArrayList;
import java.util.List;

/** Configuracion global de la plataforma, agrupada por categoria. */
@Named("configurationBean")
@ViewScoped
@Getter
@Setter
public class ConfigurationBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String CONFIG_DIALOG = "configDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<ConfigurationDto> entries = new ArrayList<>();
    private String categoryFilter;

    /** Parametro abierto en el dialog de edicion y valor propuesto. */
    private ConfigurationDto editing;
    private String editValue;

    public void load() {
        String cid = correlationId();
        String token = token();
        entries = call(() -> restClient.configuration(token, cid, categoryFilter), entries);
    }

    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    public void requestEdit(ConfigurationDto entry) {
        if (entry == null) {
            error("Seleccione el parametro a editar");
            return;
        }
        editing = entry;
        editValue = entry.getConfigValue();
    }

    public void cancelEdit() {
        editing = null;
        editValue = null;
    }

    public void save() {
        if (editing == null) {
            error("Seleccione el parametro a editar");
            return;
        }
        if (editValue == null || editValue.isBlank()) {
            error("El valor no puede quedar vacio");
            return;
        }
        String cid = correlationId();
        String token = token();
        String key = editing.getConfigKey();
        String value = editValue.trim();
        ConfigurationDto updated = call(() -> restClient.updateConfiguration(token, cid, key, value), null);
        if (updated == null) {
            return;
        }
        success("Configuracion actualizada: " + key);
        cancelEdit();
        hideDialog(CONFIG_DIALOG);
        load();
    }

    private void hideDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }
}
