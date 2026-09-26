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
public class DeviceDto {

    private UUID id;
    private UUID installationId;
    private UUID tenantId;
    private String installationName;
    private String deviceType;
    private String identifier;
    private String name;
    private String status;
    private Instant lastSeenAt;
    private Instant createdAt;
}
