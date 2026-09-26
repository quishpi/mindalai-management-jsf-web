package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.PlanDto;
import ec.mindalai.managementjsf.dto.SubscriptionDto;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Suscripciones: asignacion de plan a tenant y renovacion. */
@Named("subscriptionBean")
@ViewScoped
@Getter
@Setter
public class SubscriptionBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<SubscriptionDto> subscriptions = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<PlanDto> plans = new ArrayList<>();

    private UUID tenantFilter;
    private UUID tenantId;
    private UUID planId;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private String notes;
    private LocalDate renewUntil;
    private UUID renewingId;

    public void load() {
        String cid = correlationId();
        String token = token();
        subscriptions = call(() -> restClient.subscriptions(token, cid, tenantFilter), subscriptions);
        tenants = call(() -> restClient.tenants(token, cid, "ACTIVE", null, null), tenants);
        plans = call(() -> restClient.plans(token, cid), plans);
    }

    public void filterByTenant() {
        load();
    }

    public void create() {
        if (tenantId == null || planId == null) {
            error("Seleccione tenant y plan");
            return;
        }
        String cid = correlationId();
        String token = token();
        Instant from = atStartOfDay(validFrom);
        Instant until = atEndOfDay(validUntil);
        String note = blankToNull(notes);
        SubscriptionDto created = call(
                () -> restClient.createSubscription(token, cid, tenantId, planId, from, until, note), null);
        if (created != null) {
            success("Suscripcion creada");
            tenantId = null;
            planId = null;
            validFrom = null;
            validUntil = null;
            notes = null;
            load();
        }
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

    public void prepareRenew(SubscriptionDto subscription) {
        renewingId = subscription.getId();
        renewUntil = subscription.getValidUntil() == null ? null
                : subscription.getValidUntil().atZone(ZoneOffset.UTC).toLocalDate();
    }

    public void renew() {
        if (renewingId == null || renewUntil == null) {
            error("Indique la nueva fecha de vigencia");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = renewingId;
        Instant until = atEndOfDay(renewUntil);
        SubscriptionDto renewed = call(() -> restClient.renewSubscription(token, cid, id, until), null);
        if (renewed != null) {
            success("Suscripcion renovada");
            renewingId = null;
            renewUntil = null;
            load();
        }
    }

    public void cancelRenew() {
        renewingId = null;
        renewUntil = null;
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
