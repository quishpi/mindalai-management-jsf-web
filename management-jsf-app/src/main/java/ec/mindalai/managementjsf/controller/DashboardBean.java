package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.DashboardSummaryDto;
import ec.mindalai.managementjsf.dto.UsageSummaryDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Dashboard: estado global de la plataforma y principales consumos. */
@Named("dashboardBean")
@ViewScoped
@Getter
@Setter
public class DashboardBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    @Inject
    private PlatformRestClient restClient;

    private DashboardSummaryDto summary;
    private List<UsageSummaryDto> usageHighlights = new ArrayList<>();

    public void load() {
        String cid = correlationId();
        String token = token();
        summary = call(() -> restClient.dashboard(token, cid), summary);
        if (summary != null && summary.getUsageHighlights() != null) {
            usageHighlights = summary.getUsageHighlights();
        }
    }

    public long totalTenants() {
        return summary == null ? 0 : summary.getTotalTenants();
    }

    public long activeTenants() {
        return summary == null ? 0 : summary.getActiveTenants();
    }

    public long totalInstallations() {
        return summary == null ? 0 : summary.getTotalInstallations();
    }

    public long totalDevices() {
        return summary == null ? 0 : summary.getTotalDevices();
    }

    public long activeLicenses() {
        return summary == null ? 0 : summary.getActiveLicenses();
    }

    public long subscriptionsExpiringSoon() {
        return summary == null ? 0 : summary.getSubscriptionsExpiringSoon();
    }

    public long openTickets() {
        return summary == null ? 0 : summary.getOpenTickets();
    }

    public String formatValue(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }
}
