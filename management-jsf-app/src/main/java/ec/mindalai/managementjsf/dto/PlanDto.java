package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDto {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private int maxUsers;
    private int maxDevices;
    private Integer maxInstallations;
    private Integer maxDocumentsPerMonth;
    private boolean active;
    private List<FeatureDto> features;

    public String limits() {
        StringBuilder sb = new StringBuilder();
        sb.append(maxUsers).append(" usuarios");
        if (maxDevices > 0) {
            sb.append(" / ").append(maxDevices).append(" dispositivos");
        }
        if (maxInstallations != null && maxInstallations > 0) {
            sb.append(" / ").append(maxInstallations).append(" instalaciones");
        }
        if (maxDocumentsPerMonth != null && maxDocumentsPerMonth > 0) {
            sb.append(" / ").append(maxDocumentsPerMonth).append(" docs/mes");
        }
        return sb.toString();
    }
}
