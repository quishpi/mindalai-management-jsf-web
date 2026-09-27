package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.FeatureDto;
import ec.mindalai.managementjsf.dto.PlanDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Planes comerciales y sus features. */
@Named("planBean")
@ViewScoped
@Getter
@Setter
public class PlanBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<PlanDto> plans = new ArrayList<>();
    private List<FeatureDto> features = new ArrayList<>();
    private Set<UUID> selectedFeatures = new LinkedHashSet<>();
    private UUID selectedPlanId;

    private UUID editingId;
    private boolean editing;

    private String code;
    private String name;
    private String description;
    private BigDecimal monthlyPrice = BigDecimal.ZERO;
    private int maxUsers;
    private int maxDevices;
    private Integer maxInstallations;
    private Integer maxDocumentsPerMonth;

    public void load() {
        String cid = correlationId();
        String token = token();
        plans = call(() -> restClient.plans(token, cid), plans);
        features = call(() -> restClient.features(token, cid), features);
    }

    public void create() {
        if (isBlank(code) || isBlank(name)) {
            error("Codigo y nombre del plan son obligatorios");
            return;
        }
        String cid = correlationId();
        String token = token();
        String planCode = code.trim();
        String planName = name.trim();
        String planDescription = blankToNull(description);
        BigDecimal price = monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice;
        PlanDto created = call(() -> restClient.createPlan(token, cid, planCode, planName, planDescription,
                price, maxUsers, maxDevices, maxInstallations, maxDocumentsPerMonth), null);
        if (created != null) {
            success("Plan " + created.getCode() + " creado");
            clearForm();
            load();
        }
    }

    /** Carga el plan en el formulario de la seccion "Nuevo plan" para editarlo. */
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
    }

    /** Alternativa al create() cuando el formulario esta en modo edicion. */
    public void save() {
        if (editingId == null) {
            create();
            return;
        }
        if (isBlank(name)) {
            error("El nombre del plan es obligatorio");
            return;
        }
        String cid = correlationId();
        String token = token();
        String planName = name.trim();
        String planDescription = blankToNull(description);
        BigDecimal price = monthlyPrice == null ? BigDecimal.ZERO : monthlyPrice;
        PlanDto updated = call(() -> restClient.updatePlan(token, cid, editingId, planName, planDescription,
                price, maxUsers, maxDevices, maxInstallations, maxDocumentsPerMonth), null);
        if (updated != null) {
            success("Plan " + updated.getCode() + " actualizado");
            cancelEdit();
            load();
        }
    }

    public void cancelEdit() {
        editing = false;
        editingId = null;
        clearForm();
    }

    public void activate(PlanDto plan) {
        run(() -> {
            restClient.activatePlan(token(), correlationId(), plan.getId());
            success("Plan activado");
            load();
        });
    }

    public void deactivate(PlanDto plan) {
        run(() -> {
            restClient.deactivatePlan(token(), correlationId(), plan.getId());
            success("Plan desactivado");
            load();
        });
    }

    public void assignFeatures() {
        PlanDto plan = selectedPlan();
        if (plan == null) {
            error("Seleccione el plan al que asignar las features");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID planId = plan.getId();
        Set<UUID> features = new LinkedHashSet<>(selectedFeatures);
        PlanDto updated = call(() -> restClient.assignPlanFeatures(token, cid, planId, features), null);
        if (updated != null) {
            success("Features actualizadas para " + updated.getCode());
            load();
        }
    }

    public void selectPlan() {
        PlanDto plan = selectedPlan();
        if (plan == null) {
            selectedFeatures = new LinkedHashSet<>();
            return;
        }
        selectedFeatures = plan.getFeatures() == null ? new LinkedHashSet<>()
                : plan.getFeatures().stream().map(FeatureDto::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private PlanDto selectedPlan() {
        if (selectedPlanId == null) {
            return null;
        }
        return plans.stream().filter(plan -> plan.getId().equals(selectedPlanId)).findFirst().orElse(null);
    }

    public void toggleFeature(FeatureDto feature, boolean selected) {
        if (selected) {
            selectedFeatures.add(feature.getId());
        } else {
            selectedFeatures.remove(feature.getId());
        }
    }

    public boolean isFeatureSelected(FeatureDto feature) {
        return selectedFeatures.contains(feature.getId());
    }

    public List<FeatureDto> featuresOf(PlanDto plan) {
        return plan.getFeatures() == null ? List.of() : plan.getFeatures();
    }

    private void clearForm() {
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
        selectedPlanId = null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
