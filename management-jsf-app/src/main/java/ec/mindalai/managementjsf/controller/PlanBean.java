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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private Set<UUID> selectedFeatures = new LinkedHashSet<>();

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
    }

    public void openNew() {
        resetForm();
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
        selectedFeatures = plan.getFeatures() == null ? new LinkedHashSet<>()
                : plan.getFeatures().stream().map(FeatureDto::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
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
        if (selectedFeatures.isEmpty()) {
            error("Seleccione al menos una feature del plan");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = editingId;
        String planName = name.trim();
        String planDescription = blankToNull(description);
        BigDecimal price = monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice;
        Set<UUID> features = new LinkedHashSet<>(selectedFeatures);
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
        changeStatus(plan, () -> restClient.activatePlan(token(), correlationId(), plan.getId()),
                "Plan " + plan.getCode() + " activado");
    }

    public void deactivate(PlanDto plan) {
        changeStatus(plan, () -> restClient.deactivatePlan(token(), correlationId(), plan.getId()),
                "Plan " + plan.getCode() + " desactivado");
    }

    public String statusSeverity(String status) {
        return STATUS_ACTIVE.equals(status) ? "success" : "secondary";
    }

    public List<FeatureDto> featuresOf(PlanDto plan) {
        return plan.getFeatures() == null ? List.of() : plan.getFeatures();
    }

    public boolean isFeatureSelected(FeatureDto feature) {
        return selectedFeatures.contains(feature.getId());
    }

    public void toggleFeature(FeatureDto feature, boolean selected) {
        if (selected) {
            selectedFeatures.add(feature.getId());
        } else {
            selectedFeatures.remove(feature.getId());
        }
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
        if (selectedFeatures.isEmpty()) {
            error("Seleccione al menos una feature del plan");
            return;
        }
        String cid = correlationId();
        String token = token();
        String planCode = code.trim();
        String planName = name.trim();
        String planDescription = blankToNull(description);
        BigDecimal price = monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice;
        Set<UUID> features = new LinkedHashSet<>(selectedFeatures);
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

    private void changeStatus(PlanDto plan, Runnable operation, String message) {
        run(() -> {
            operation.run();
            success(message);
            load();
        });
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
        selectedFeatures = new LinkedHashSet<>();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
