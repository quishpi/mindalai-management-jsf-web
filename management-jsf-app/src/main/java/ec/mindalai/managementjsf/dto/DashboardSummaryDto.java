package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private long totalTenants;
    private long activeTenants;
    private long totalInstallations;
    private long totalDevices;
    private long activeLicenses;
    private long subscriptionsExpiringSoon;
    private long openTickets;
    private List<UsageSummaryDto> usageHighlights;
}
