package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageDto {

    private UUID id;
    private UUID tenantId;
    private UUID installationId;
    private String metricName;
    private BigDecimal metricValue;
    private Instant recordedAt;
}
