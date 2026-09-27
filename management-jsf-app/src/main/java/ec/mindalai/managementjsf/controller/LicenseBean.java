package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.LicenseDto;
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

/** Licencias: emision sobre una suscripcion activa y revocacion. */
@Named("licenseBean")
@ViewScoped
@Getter
@Setter
public class LicenseBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<LicenseDto> licenses = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<SubscriptionDto> subscriptions = new ArrayList<>();

    private UUID tenantFilter;
    private UUID tenantId;
    private UUID subscriptionId;
    private LocalDate validFrom;
    private LocalDate validUntil;

    private String revokeReason;
    private UUID revokingId;

    public void load() {
        String cid = correlationId();
        String token = token();
        licenses = call(() -> restClient.licenses(token, cid, tenantFilter), licenses);
        tenants = call(() -> restClient.tenants(token, cid, "ACTIVE", null, null), tenants);
        subscriptions = call(() -> restClient.subscriptions(token, cid, tenantId), subscriptions);
    }

    /** Los selectores solo ofrecen tenants y suscripciones activos. */
    public List<TenantDto> activeTenants() {
        return tenants.stream().filter(tenant -> "ACTIVE".equals(tenant.getStatus())).toList();
    }

    public List<SubscriptionDto> activeSubscriptions() {
        return subscriptions.stream().filter(subscription -> "ACTIVE".equals(subscription.getStatus())).toList();
    }

    public void onTenantChange() {
        subscriptions = call(() -> restClient.subscriptions(token(), correlationId(), tenantId), subscriptions);
        subscriptionId = activeSubscriptions().stream().findFirst().map(SubscriptionDto::getId).orElse(null);
    }

    public void issue() {
        if (tenantId == null || subscriptionId == null) {
            error("Seleccione tenant y suscripcion");
            return;
        }
        String cid = correlationId();
        String token = token();
        Instant from = atStartOfDay(validFrom);
        Instant until = atEndOfDay(validUntil);
        LicenseDto issued = call(() -> restClient.issueLicense(token, cid, tenantId, subscriptionId, from, until), null);
        if (issued != null) {
            success("Licencia emitida: " + issued.getLicenseKey());
            validFrom = null;
            validUntil = null;
            load();
        }
    }

    public void prepareRevoke(LicenseDto license) {
        revokingId = license.getId();
        revokeReason = null;
    }

    public void revoke() {
        if (revokingId == null) {
            error("Seleccione la licencia a revocar");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = revokingId;
        String reason = revokeReason == null || revokeReason.isBlank() ? null : revokeReason.trim();
        LicenseDto revoked = call(() -> restClient.revokeLicense(token, cid, id, reason), null);
        if (revoked != null) {
            success("Licencia revocada");
            revokingId = null;
            revokeReason = null;
            load();
        }
    }

    public void cancelRevoke() {
        revokingId = null;
        revokeReason = null;
    }

    private Instant atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }
}
