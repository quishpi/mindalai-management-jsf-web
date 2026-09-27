package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.FeatureDto;
import ec.mindalai.managementjsf.dto.PlanDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Planes comerciales del catalogo. Un plan exige al menos una feature, asi que la asignacion
 * se resuelve dentro del mismo dialog de alta/edicion y no como una accion aparte.
 */
@Named("planBean")
@ViewScoped
@Getter
@Setter
public class PlanBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String STATUS_ACTIVE = "ACTIVE";

    private static final String PLAN_DIALOG = "planDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<PlanDto> plans = new ArrayList<>();
    private List<FeatureDto> features = new ArrayList<>();
    /** Una casilla por feature del catalogo, con su marca actual: es el modelo del dialog. */
    private List<FeatureOption> featureOptions = new ArrayList<>();

    private String code;
    private String name;
    private String description;
    private BigDecimal monthlyPrice = BigDecimal.ZERO;
    private int maxUsers;
    private int maxDevices;
    private Integer maxInstallations;
    private Integer maxDocumentsPerMonth;

    private UUID editingId;
    private boolean editing;

    /** Numero de orden de una fila, continuo entre paginas. */
    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        plans = call(() -> restClient.plans(token, cid), plans);
        features = call(() -> restClient.features(token, cid), features);
        // Las casillas se arman con el catalogo para que el dialog abra siempre con contenido,
        // incluso la primera vez que se muestra la pagina.
        buildFeatureOptions(null);
    }

    public void openNew() {
        resetForm();
        buildFeatureOptions(null);
    }

    public void edit(PlanDto plan) {
        editing = true;
        editingId = plan.getId();
        code = plan.getCode();
        name = plan.getName();
        description = plan.getDescription();
        monthlyPrice = plan.getMonthlyPrice();
        maxUsers = plan.getMaxUsers();
        maxDevices = plan.getMaxDevices();
        maxInstallations = plan.getMaxInstallations();
        maxDocumentsPerMonth = plan.getMaxDocumentsPerMonth();
        buildFeatureOptions(plan.getFeatures());
    }

    public void save() {
        if (editingId == null) {
            create();
            return;
        }
        if (isBlank(name)) {
            error("El nombre del plan es obligatorio");
            return;
        }
        Set<UUID> features = selectedFeatureIds();
        if (features.isEmpty()) {
            error("Seleccione al menos una feature del plan");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = editingId;
        String planName = name.trim();
        String planDescription = blankToNull(description);
        BigDecimal price = monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice;
        PlanDto updated = call(() -> restClient.updatePlan(token, cid, id, planName, planDescription, price,
                maxUsers, maxDevices, maxInstallations, maxDocumentsPerMonth, features), null, this::duplicatedMessage);
        if (updated == null) {
            return;
        }
        success("Plan " + updated.getCode() + " actualizado");
        replaceInPlace(updated);
        resetForm();
        hideDialog(PLAN_DIALOG);
    }

    public void activate(PlanDto plan) {
        changeStatus(plan, true);
    }

    public void deactivate(PlanDto plan) {
        changeStatus(plan, false);
    }

    public String statusSeverity(String status) {
        return STATUS_ACTIVE.equals(status) ? "success" : "secondary";
    }

    /** Ids de las casillas marcadas, en el orden del catalogo. */
    public Set<UUID> selectedFeatureIds() {
        Set<UUID> selected = new LinkedHashSet<>();
        for (FeatureOption option : featureOptions) {
            if (option.isSelected()) {
                selected.add(option.getFeature().getId());
            }
        }
        return selected;
    }

    /** Los limites sin tope se dejan vacios para no convertir un 0 en un bloqueo real. */
    public int unbounded(Integer value) {
        return value == null || value <= 0 ? 1 : 0;
    }

    private void create() {
        if (isBlank(code) || isBlank(name)) {
            error("Codigo y nombre del plan son obligatorios");
            return;
        }
        Set<UUID> features = selectedFeatureIds();
        if (features.isEmpty()) {
            error("Seleccione al menos una feature del plan");
            return;
        }
        String cid = correlationId();
        String token = token();
        String planCode = code.trim();
        String planName = name.trim();
        String planDescription = blankToNull(description);
        BigDecimal price = monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice;
        PlanDto created = call(() -> restClient.createPlan(token, cid, planCode, planName, planDescription, price,
                maxUsers, maxDevices, maxInstallations, maxDocumentsPerMonth, features), null,
                this::duplicatedMessage);
        if (created == null) {
            return;
        }
        success("Plan " + created.getCode() + " creado");
        resetForm();
        hideDialog(PLAN_DIALOG);
        load();
    }

    /**
     * Activa o desactiva el plan y sustituye la fila por la version devuelta por el API. Asi la
     * tabla refleja el cambio sin recargar y sin reordenarse.
     */
    private void changeStatus(PlanDto plan, boolean activate) {
        String cid = correlationId();
        String token = token();
        UUID id = plan.getId();
        PlanDto updated = call(() -> activate ? restClient.activatePlan(token, cid, id)
                : restClient.deactivatePlan(token, cid, id), null);
        if (updated == null) {
            return;
        }
        success("Plan " + updated.getCode() + (activate ? " activado" : " desactivado"));
        replaceInPlace(updated);
    }

    /** Traduce el 409 del API a un mensaje que senale el dato duplicado concreto. */
    private String duplicatedMessage(PlatformApiException ex) {
        String detail = ex.getError().detailText();
        if ("PLAN_DUPLICATED_CODE".equals(detail)) {
            return "Ya existe un plan con el codigo " + code.trim() + ". Use otro codigo.";
        }
        if ("PLAN_DUPLICATED_NAME".equals(detail)) {
            return "Ya existe un plan con el nombre " + name.trim() + ". Use otro nombre.";
        }
        return ex.getMessage();
    }

    private void replaceInPlace(PlanDto updated) {
        for (int i = 0; i < plans.size(); i++) {
            if (updated.getId().equals(plans.get(i).getId())) {
                plans.set(i, updated);
                return;
            }
        }
        plans.add(0, updated);
    }

    private void hideDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }

    private void resetForm() {
        editing = false;
        editingId = null;
        code = null;
        name = null;
        description = null;
        monthlyPrice = BigDecimal.ZERO;
        maxUsers = 0;
        maxDevices = 0;
        maxInstallations = null;
        maxDocumentsPerMonth = null;
    }

    /** Reconstruye las casillas marcando las features ya asignadas al plan (todas, si es null). */
    private void buildFeatureOptions(Collection<FeatureDto> assigned) {
        Set<UUID> assignedIds = new LinkedHashSet<>();
        if (assigned != null) {
            for (FeatureDto feature : assigned) {
                assignedIds.add(feature.getId());
            }
        }
        featureOptions = new ArrayList<>();
        for (FeatureDto feature : features) {
            featureOptions.add(new FeatureOption(feature, assignedIds.contains(feature.getId())));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    /**
     * Feature del catalogo con su casilla marcada. El dialog trabaja sobre esta lista en vez de
     * sobre un Set de UUID para que cada checkbox tenga una propiedad booleana real que JSF
     * pueda leer y escribir al enviar el formulario.
     */
    @Getter
    @Setter
    public static class FeatureOption implements Serializable {

        private static final long serialVersionUID = 1L;

        private final FeatureDto feature;
        private boolean selected;

        public FeatureOption(FeatureDto feature, boolean selected) {
            this.feature = feature;
            this.selected = selected;
        }

        public String label() {
            return feature.getName() + " (" + feature.getCode() + ")";
        }
    }
}
