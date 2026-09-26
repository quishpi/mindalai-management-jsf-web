package ec.mindalai.managementjsf.client;

import ec.mindalai.managementjsf.dto.AuditEntryDto;
import ec.mindalai.managementjsf.dto.ConfigurationDto;
import ec.mindalai.managementjsf.dto.DashboardSummaryDto;
import ec.mindalai.managementjsf.dto.DeviceDto;
import ec.mindalai.managementjsf.dto.FeatureDto;
import ec.mindalai.managementjsf.dto.InstallationDto;
import ec.mindalai.managementjsf.dto.LicenseDto;
import ec.mindalai.managementjsf.dto.PageResult;
import ec.mindalai.managementjsf.dto.PlanDto;
import ec.mindalai.managementjsf.dto.SubscriptionDto;
import ec.mindalai.managementjsf.dto.SupportTicketDto;
import ec.mindalai.managementjsf.dto.TenantDto;
import ec.mindalai.managementjsf.dto.UsageDto;
import ec.mindalai.managementjsf.dto.UsageSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cliente de los recursos del plano de plataforma. Solo translate DTOs: no contiene
 * reglas de negocio (Regla 7/8). Reutiliza {@link PlatformApiClient} para cabeceras,
 * timeouts, correlation id y mapeo de errores.
 */
@Service
@RequiredArgsConstructor
public class PlatformRestClient {

    private final PlatformApiClient api;

    // ---------------------------------------------------------------- dashboard

    public DashboardSummaryDto dashboard(String token, String cid) {
        return api.get(path("/dashboard/summary"), token, cid, DashboardSummaryDto.class);
    }

    // ------------------------------------------------------------------ tenants

    public List<TenantDto> tenants(String token, String cid, String status, String search, UUID installationId) {
        return api.getList(path("/tenants"), token, cid, TenantDto[].class,
                PlatformApiClient.body("status", status, "search", search, "installationId", installationId));
    }

    public TenantDto createTenant(String token, String cid, String legalName, String ruc, String tradeName,
                                  String email, String phone) {
        return api.post(path("/tenants"), token, cid, PlatformApiClient.body(
                "legalName", legalName, "ruc", ruc, "tradeName", tradeName,
                "email", email, "phone", phone), TenantDto.class);
    }

    public TenantDto updateTenant(String token, String cid, UUID id, String legalName, String tradeName,
                                  String email, String phone) {
        return api.put(path("/tenants/" + id), token, cid, PlatformApiClient.body(
                "legalName", legalName, "tradeName", tradeName, "email", email, "phone", phone), TenantDto.class);
    }

    public TenantDto suspendTenant(String token, String cid, UUID id) {
        return api.post(path("/tenants/" + id + "/suspend"), token, cid, null, TenantDto.class);
    }

    public TenantDto reactivateTenant(String token, String cid, UUID id) {
        return api.post(path("/tenants/" + id + "/reactivate"), token, cid, null, TenantDto.class);
    }

    public TenantDto deactivateTenant(String token, String cid, UUID id) {
        return api.delete(path("/tenants/" + id), token, cid, TenantDto.class);
    }

    // -------------------------------------------------------------------- plans

    public List<PlanDto> plans(String token, String cid) {
        return api.getList(path("/plans"), token, cid, PlanDto[].class, Map.of());
    }

    public List<FeatureDto> features(String token, String cid) {
        return api.getList(path("/plans/features"), token, cid, FeatureDto[].class, Map.of());
    }

    public PlanDto createPlan(String token, String cid, String code, String name, String description,
                              BigDecimal monthlyPrice, int maxUsers, int maxDevices, Integer maxInstallations,
                              Integer maxDocumentsPerMonth) {
        return api.post(path("/plans"), token, cid, PlatformApiClient.body(
                "code", code, "name", name, "description", description, "monthlyPrice", monthlyPrice,
                "maxUsers", maxUsers, "maxDevices", maxDevices, "maxInstallations", maxInstallations,
                "maxDocumentsPerMonth", maxDocumentsPerMonth), PlanDto.class);
    }

    public PlanDto activatePlan(String token, String cid, UUID id) {
        return api.post(path("/plans/" + id + "/activate"), token, cid, null, PlanDto.class);
    }

    public PlanDto deactivatePlan(String token, String cid, UUID id) {
        return api.post(path("/plans/" + id + "/deactivate"), token, cid, null, PlanDto.class);
    }

    public PlanDto assignPlanFeatures(String token, String cid, UUID id, Set<UUID> featureIds) {
        return api.put(path("/plans/" + id + "/features"), token, cid,
                PlatformApiClient.body("featureIds", featureIds), PlanDto.class);
    }

    // ------------------------------------------------------------- subscriptions

    public List<SubscriptionDto> subscriptions(String token, String cid, UUID tenantId) {
        return api.getList(path("/subscriptions"), token, cid, SubscriptionDto[].class,
                PlatformApiClient.body("tenantId", tenantId));
    }

    public SubscriptionDto createSubscription(String token, String cid, UUID tenantId, UUID planId,
                                              Instant validFrom, Instant validUntil, String notes) {
        return api.post(path("/subscriptions"), token, cid, PlatformApiClient.body(
                "tenantId", tenantId, "planId", planId, "validFrom", validFrom,
                "validUntil", validUntil, "notes", notes), SubscriptionDto.class);
    }

    public SubscriptionDto suspendSubscription(String token, String cid, UUID id) {
        return api.post(path("/subscriptions/" + id + "/suspend"), token, cid, null, SubscriptionDto.class);
    }

    public SubscriptionDto cancelSubscription(String token, String cid, UUID id) {
        return api.post(path("/subscriptions/" + id + "/cancel"), token, cid, null, SubscriptionDto.class);
    }

    public SubscriptionDto renewSubscription(String token, String cid, UUID id, Instant validUntil) {
        return api.put(path("/subscriptions/" + id + "/renew"), token, cid,
                PlatformApiClient.body("validUntil", validUntil), SubscriptionDto.class);
    }

    // ----------------------------------------------------------------- licenses

    public List<LicenseDto> licenses(String token, String cid, UUID tenantId) {
        return api.getList(path("/licenses"), token, cid, LicenseDto[].class,
                PlatformApiClient.body("tenantId", tenantId));
    }

    public LicenseDto issueLicense(String token, String cid, UUID tenantId, UUID subscriptionId,
                                  Instant validFrom, Instant validUntil) {
        return api.post(path("/licenses"), token, cid, PlatformApiClient.body(
                "tenantId", tenantId, "subscriptionId", subscriptionId,
                "validFrom", validFrom, "validUntil", validUntil), LicenseDto.class);
    }

    public LicenseDto revokeLicense(String token, String cid, UUID id, String reason) {
        return api.post(path("/licenses/" + id + "/revoke"), token, cid,
                PlatformApiClient.body("reason", reason), LicenseDto.class);
    }

    // ------------------------------------------------------------- installations

    public List<InstallationDto> installations(String token, String cid, UUID tenantId) {
        return api.getList(path("/installations"), token, cid, InstallationDto[].class,
                PlatformApiClient.body("tenantId", tenantId));
    }

    public InstallationDto createInstallation(String token, String cid, UUID tenantId, String name,
                                              String address, String contactName) {
        return api.post(path("/installations"), token, cid, PlatformApiClient.body(
                "tenantId", tenantId, "name", name, "address", address,
                "contactName", contactName), InstallationDto.class);
    }

    public InstallationDto setInstallationMaintenance(String token, String cid, UUID id, boolean maintenance) {
        return api.post(path("/installations/" + id + "/maintenance"), token, cid,
                PlatformApiClient.body("maintenance", maintenance), InstallationDto.class);
    }

    public InstallationDto deactivateInstallation(String token, String cid, UUID id) {
        return api.delete(path("/installations/" + id), token, cid, InstallationDto.class);
    }

    // ------------------------------------------------------------------ devices

    public List<DeviceDto> devices(String token, String cid, UUID installationId) {
        return api.getList(path("/devices"), token, cid, DeviceDto[].class,
                PlatformApiClient.body("installationId", installationId));
    }

    public DeviceDto registerDevice(String token, String cid, UUID installationId, String deviceType,
                                    String identifier, String name) {
        return api.post(path("/devices"), token, cid, PlatformApiClient.body(
                "installationId", installationId, "deviceType", deviceType,
                "identifier", identifier, "name", name), DeviceDto.class);
    }

    public DeviceDto blockDevice(String token, String cid, UUID id) {
        return api.post(path("/devices/" + id + "/block"), token, cid, null, DeviceDto.class);
    }

    public DeviceDto activateDevice(String token, String cid, UUID id) {
        return api.post(path("/devices/" + id + "/activate"), token, cid, null, DeviceDto.class);
    }

    public DeviceDto retireDevice(String token, String cid, UUID id) {
        return api.post(path("/devices/" + id + "/retire"), token, cid, null, DeviceDto.class);
    }

    public DeviceDto deviceHeartbeat(String token, String cid, UUID id) {
        return api.post(path("/devices/" + id + "/heartbeat"), token, cid, null, DeviceDto.class);
    }

    // --------------------------------------------------------------------- usage

    public List<UsageDto> usage(String token, String cid, UUID tenantId, Instant from, Instant to) {
        return api.getList(path("/usage"), token, cid, UsageDto[].class,
                PlatformApiClient.body("tenantId", tenantId, "from", from, "to", to));
    }

    public List<UsageSummaryDto> usageSummary(String token, String cid, Instant from, Instant to) {
        return api.getList(path("/usage/summary"), token, cid, UsageSummaryDto[].class,
                PlatformApiClient.body("from", from, "to", to));
    }

    public List<UsageDto> latestUsage(String token, String cid, int limit) {
        return api.getList(path("/usage/latest"), token, cid, UsageDto[].class,
                PlatformApiClient.body("limit", limit));
    }

    public UsageDto recordUsage(String token, String cid, UUID tenantId, UUID installationId,
                                String metricName, BigDecimal metricValue, Instant recordedAt) {
        return api.post(path("/usage"), token, cid, PlatformApiClient.body(
                "tenantId", tenantId, "installationId", installationId, "metricName", metricName,
                "metricValue", metricValue, "recordedAt", recordedAt), UsageDto.class);
    }

    // ------------------------------------------------------------------ support

    public List<SupportTicketDto> tickets(String token, String cid, UUID tenantId) {
        return api.getList(path("/support/tickets"), token, cid, SupportTicketDto[].class,
                PlatformApiClient.body("tenantId", tenantId));
    }

    public SupportTicketDto openTicket(String token, String cid, UUID tenantId, UUID installationId,
                                       String subject, String description, String priority) {
        return api.post(path("/support/tickets"), token, cid, PlatformApiClient.body(
                "tenantId", tenantId, "installationId", installationId, "subject", subject,
                "description", description, "priority", priority), SupportTicketDto.class);
    }

    public SupportTicketDto assignTicket(String token, String cid, UUID id, UUID assignedTo) {
        return api.post(path("/support/tickets/" + id + "/assign"), token, cid,
                PlatformApiClient.body("assignedTo", assignedTo), SupportTicketDto.class);
    }

    public SupportTicketDto resolveTicket(String token, String cid, UUID id) {
        return api.post(path("/support/tickets/" + id + "/resolve"), token, cid, null, SupportTicketDto.class);
    }

    public SupportTicketDto closeTicket(String token, String cid, UUID id) {
        return api.post(path("/support/tickets/" + id + "/close"), token, cid, null, SupportTicketDto.class);
    }

    // ------------------------------------------------------------ configuration

    public List<ConfigurationDto> configuration(String token, String cid, String category) {
        return api.getList(path("/configuration"), token, cid, ConfigurationDto[].class,
                PlatformApiClient.body("category", category));
    }

    public ConfigurationDto updateConfiguration(String token, String cid, String key, String value) {
        return api.put(path("/configuration/" + key), token, cid,
                PlatformApiClient.body("value", value), ConfigurationDto.class);
    }

    // -------------------------------------------------------------------- audit

    public PageResult<AuditEntryDto> audit(String token, String cid, Map<String, Object> query) {
        return api.get(path("/audit"), token, cid,
                new ParameterizedTypeReference<PageResult<AuditEntryDto>>() {
                }, query);
    }

    private static String path(String suffix) {
        return PlatformApiClient.platformPath(suffix);
    }
}
