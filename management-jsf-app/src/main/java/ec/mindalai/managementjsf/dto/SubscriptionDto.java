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
public class SubscriptionDto {

    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private UUID planId;
    private String planName;
    private String planCode;
    private String status;
    private Instant validFrom;
    private Instant validUntil;
    private Instant cancelledAt;
    private String notes;
}
