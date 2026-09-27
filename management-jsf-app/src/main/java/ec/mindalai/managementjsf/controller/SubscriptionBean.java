package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.PlanDto;
import ec.mindalai.managementjsf.dto.SubscriptionDto;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Suscripciones: asignacion de plan a tenant y renovacion. Un tenant admite varias
 * suscripciones a lo largo del tiempo, pero solo una ACTIVE: por eso el alta avisa antes de
 * llamar al API y la renovacion se abre en un dialog propio con la vigencia vigente.
 */
@Named("subscriptionBean")
@ViewScoped
@Getter
@Setter
public class SubscriptionBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private static final String SUBSCRIPTION_DIALOG = "subscriptionDialogWidget";
    private static final String RENEW_DIALOG = "renewDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<SubscriptionDto> subscriptions = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<PlanDto> plans = new ArrayList<>();

    private UUID tenantFilter;
    private String statusFilter;

    private UUID tenantId;
    private UUID planId;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String notes;

    /** Suscripcion sobre la que se pidio renovar y nueva fecha de vigencia. */
    private SubscriptionDto renewing;
    private LocalDate renewUntil;

    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    /**
     * El API no expone un filtro de estado para suscripciones, asi que el recorte es de vista:
     * se aplica sobre la lista ya cargada y por eso tambien es el contenido que se exporta.
     */
    public List<SubscriptionDto> visibleSubscriptions() {
        if (statusFilter == null || statusFilter.isBlank()) {
            return subscriptions;
        }
        return subscriptions.stream()
                .filter(subscription -> statusFilter.equals(subscription.getStatus()))
                .toList();
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        subscriptions = call(() -> restClient.subscriptions(token, cid, tenantFilter), subscriptions);
        tenants = call(() -> restClient.tenants(token, cid, "ACTIVE", null, null), tenants);
        plans = call(() -> restClient.plans(token, cid), plans);
    }

    public void openNew() {
        tenantId = null;
        planId = null;
        validFrom = null;
        validUntil = null;
        notes = null;
    }

    public void save() {
        if (tenantId == null || planId == null) {
            error("Seleccione tenant y plan");
            return;
        }
        if (tenantHasActivePlan(tenantId)) {
            error("El tenant ya tiene un plan activo: suspenda o cancele la suscripcion vigente antes de crear otra");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID tenant = tenantId;
        UUID plan = planId;
        Instant from = atStartOfDay(validFrom);
        Instant until = atEndOfDay(validUntil);
        String note = blankToNull(notes);
        SubscriptionDto created = call(
                () -> restClient.createSubscription(token, cid, tenant, plan, from, until, note), null,
                this::activePlanMessage);
        if (created == null) {
            return;
        }
        success("Suscripcion creada para " + tenantName(tenant));
        openNew();
        hideDialog(SUBSCRIPTION_DIALOG);
        load();
    }

    public void suspend(SubscriptionDto subscription) {
        run(() -> {
            restClient.suspendSubscription(token(), correlationId(), subscription.getId());
            success("Suscripcion suspendida");
            load();
        });
    }

    public void cancel(SubscriptionDto subscription) {
        run(() -> {
            restClient.cancelSubscription(token(), correlationId(), subscription.getId());
            success("Suscripcion cancelada");
            load();
        });
    }

    public void requestRenew(SubscriptionDto subscription) {
        renewing = subscription;
        renewUntil = subscription.getValidUntil() == null ? null
                : subscription.getValidUntil().atZone(ZoneOffset.UTC).toLocalDate();
    }

    public void confirmRenew() {
        if (renewing == null || renewUntil == null) {
            error("Indique la nueva fecha de vigencia");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = renewing.getId();
        Instant until = atEndOfDay(renewUntil);
        SubscriptionDto renewed = call(() -> restClient.renewSubscription(token, cid, id, until), null,
                this::activePlanMessage);
        if (renewed == null) {
            return;
        }
        success("Suscripcion renovada hasta el " + renewUntil);
        renewing = null;
        renewUntil = null;
        hideDialog(RENEW_DIALOG);
        load();
    }

    /** El selector de plan solo ofrece planes activos. */
    public List<PlanDto> activePlans() {
        return plans.stream().filter(PlanDto::isActive).toList();
    }

    /**
     * Un tenant admite varias suscripciones, pero solo una ACTIVE. Avisa antes de llamar al
     * API para explicar la regla sin esperar el 409.
     */
    public boolean tenantHasActivePlan(UUID tenant) {
        if (tenant == null) {
            return false;
        }
        return subscriptions.stream()
                .anyMatch(subscription -> STATUS_ACTIVE.equals(subscription.getStatus())
                        && tenant.equals(subscription.getTenantId()));
    }

    public String statusSeverity(String status) {
        if (STATUS_ACTIVE.equals(status)) {
            return "success";
        }
        return STATUS_SUSPENDED.equals(status) ? "warning" : "secondary";
    }

    public String tenantName(UUID tenant) {
        if (tenant == null) {
            return "-";
        }
        return tenants.stream()
                .filter(candidate -> tenant.equals(candidate.getId()))
                .map(TenantDto::getTradeName)
                .findFirst()
                .orElseGet(() -> tenant.toString());
    }

    private String activePlanMessage(PlatformApiException ex) {
        if ("TENANT_ALREADY_HAS_ACTIVE_PLAN".equals(ex.getError().detailText())) {
            return "El tenant ya tiene un plan activo: suspenda o cancele la suscripcion vigente antes de crear otra";
        }
        return ex.getMessage();
    }

    private void hideDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }

    private Instant atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
