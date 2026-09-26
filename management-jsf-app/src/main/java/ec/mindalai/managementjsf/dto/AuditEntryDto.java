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
public class AuditEntryDto {

    private UUID id;
    private UUID userId;
    private String username;
    private UUID tenantId;
    private UUID installationId;
    private String action;
    private String resourceType;
    private UUID resourceId;
    private String result;
    private Instant occurredAt;
    private String ipAddress;
    private String metadata;
}
