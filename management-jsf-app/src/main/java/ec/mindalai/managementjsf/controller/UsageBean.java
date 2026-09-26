package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.InstallationDto;
import ec.mindalai.managementjsf.dto.TenantDto;
import ec.mindalai.managementjsf.dto.UsageDto;
import ec.mindalai.managementjsf.dto.UsageSummaryDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Consumo: eventos ingested, resumen agregado y ultimos registros. */
@Named("usageBean")
@ViewScoped
@Getter
@Setter
public class UsageBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private List<UsageDto> latest = new ArrayList<>();
    private List<UsageSummaryDto> summary = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<InstallationDto> installations = new ArrayList<>();

    private LocalDate from;
    private LocalDate to;
    private UUID tenantFilter;

    private UUID tenantId;
    private UUID installationId;
    private String metricName;
    private BigDecimal metricValue;

    public void load() {
        String cid = correlationId();
        String token = token();
        Instant fromInstant = atStartOfDay(from);
        Instant toInstant = atEndOfDay(to);
        latest = call(() -> restClient.latestUsage(token, cid, 50), latest);
        summary = call(() -> restClient.usageSummary(token, cid, fromInstant, toInstant), summary);
        tenants = call(() -> restClient.tenants(token, cid, null, null, null), tenants);
        if (tenantId != null) {
            installations = call(() -> restClient.installations(token, cid, tenantId), installations);
        }
    }

    public void onTenantChange() {
        String cid = correlationId();
        String token = token();
        installations = call(() -> restClient.installations(token, cid, tenantId), installations);
    }

    public void record() {
        if (tenantId == null || metricName == null || metricName.isBlank() || metricValue == null) {
            error("Complete tenant, metrica y valor");
            return;
        }
        String cid = correlationId();
        String token = token();
        String metric = metricName.trim();
        BigDecimal value = metricValue;
        UsageDto recorded = call(
                () -> restClient.recordUsage(token, cid, tenantId, installationId, metric, value, null), null);
        if (recorded != null) {
            success("Consumo registrado");
            metricName = null;
            metricValue = null;
            load();
        }
    }

    private Instant atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }
}
