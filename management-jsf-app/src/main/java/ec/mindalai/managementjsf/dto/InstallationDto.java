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
public class InstallationDto {

    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private String name;
    private String address;
    private String contactName;
    private String status;
    private int deviceCount;
    private Instant createdAt;
    private Instant updatedAt;
}
