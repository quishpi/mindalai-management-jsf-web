package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseDto {

    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private UUID subscriptionId;
    private String status;
    private String licenseKey;
    private Instant validFrom;
    private Instant validUntil;
    private Instant revokedAt;
    private String revocationReason;
}
