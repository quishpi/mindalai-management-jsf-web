package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformApiException;
import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.LicenseDto;
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
 * Licencias: emision sobre una suscripcion activa y revocacion. Un tenant admite varias
 * licencias a lo largo del tiempo, pero solo una ACTIVE, de modo que emitir exige revocar
 * antes la vigente.
 */
@Named("licenseBean")
@ViewScoped
@Getter
@Setter
public class LicenseBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REVOKED = "REVOKED";

    private static final String LICENSE_DIALOG = "licenseDialogWidget";
    private static final String REVOKE_DIALOG = "revokeDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<LicenseDto> licenses = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<SubscriptionDto> subscriptions = new ArrayList<>();

    private UUID tenantFilter;
    private String statusFilter;

    private UUID tenantId;
    private UUID subscriptionId;
    private LocalDate validFrom;
    private LocalDate validUntil;

    /** Licencia a revocar y motivo, con el dialog abierto. */
    private LicenseDto revoking;
    private String revokeReason;

    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    /**
     * El API no expone un filtro de estado para licencias, asi que el recorte es de vista:
     * se aplica sobre la lista ya cargada y por eso tambien es el contenido que se exporta.
     */
    public List<LicenseDto> visibleLicenses() {
        if (statusFilter == null || statusFilter.isBlank()) {
            return licenses;
        }
        return licenses.stream()
                .filter(license -> statusFilter.equals(license.getStatus()))
                .toList();
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        licenses = call(() -> restClient.licenses(token, cid, tenantFilter), licenses);
        tenants = call(() -> restClient.tenants(token, cid, "ACTIVE", null, null), tenants);
        subscriptions = call(() -> restClient.subscriptions(token, cid, tenantId), subscriptions);
    }

    public void openNew() {
        tenantId = tenantFilter;
        subscriptionId = null;
        validFrom = null;
        validUntil = null;
        onTenantChange();
    }

    public void save() {
        if (tenantId == null || subscriptionId == null) {
            error("Seleccione tenant y suscripcion");
            return;
        }
        if (tenantHasActiveLicense(tenantId)) {
            error("El tenant ya tiene una licencia activa: revoquela antes de emitir otra");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID tenant = tenantId;
        UUID subscription = subscriptionId;
        Instant from = atStartOfDay(validFrom);
        Instant until = atEndOfDay(validUntil);
        LicenseDto issued = call(() -> restClient.issueLicense(token, cid, tenant, subscription, from, until), null,
                this::activeLicenseMessage);
        if (issued == null) {
            return;
        }
        success("Licencia emitida: " + issued.getLicenseKey());
        validFrom = null;
        validUntil = null;
        hideDialog(LICENSE_DIALOG);
        load();
    }

    public void requestRevoke(LicenseDto license) {
        revoking = license;
        revokeReason = null;
    }

    public void confirmRevoke() {
        if (revoking == null) {
            error("Seleccione la licencia a revocar");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = revoking.getId();
        String reason = revokeReason == null || revokeReason.isBlank() ? null : revokeReason.trim();
        LicenseDto revoked = call(() -> restClient.revokeLicense(token, cid, id, reason), null);
        if (revoked == null) {
            return;
        }
        success("Licencia revocada");
        revoking = null;
        revokeReason = null;
        hideDialog(REVOKE_DIALOG);
        load();
    }

    public void onTenantChange() {
        subscriptions = call(() -> restClient.subscriptions(token(), correlationId(), tenantId), subscriptions);
        subscriptionId = activeSubscriptions().stream().findFirst().map(SubscriptionDto::getId).orElse(null);
    }

    /** Los selectores solo ofrecen tenants y suscripciones activos. */
    public List<TenantDto> activeTenants() {
        return tenants.stream().filter(tenant -> STATUS_ACTIVE.equals(tenant.getStatus())).toList();
    }

    public List<SubscriptionDto> activeSubscriptions() {
        return subscriptions.stream().filter(subscription -> STATUS_ACTIVE.equals(subscription.getStatus())).toList();
    }

    public boolean tenantHasActiveLicense(UUID tenant) {
        if (tenant == null) {
            return false;
        }
        return licenses.stream()
                .anyMatch(license -> STATUS_ACTIVE.equals(license.getStatus())
                        && tenant.equals(license.getTenantId()));
    }

    public String statusSeverity(String status) {
        if (STATUS_ACTIVE.equals(status)) {
            return "success";
        }
        return STATUS_REVOKED.equals(status) ? "danger" : "secondary";
    }

    private String activeLicenseMessage(PlatformApiException ex) {
        if ("TENANT_ALREADY_HAS_LICENSE".equals(ex.getError().detailText())) {
            return "El tenant ya tiene una licencia activa: revoquela antes de emitir otra";
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
}
