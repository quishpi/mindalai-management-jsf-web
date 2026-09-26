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
public class SupportTicketDto {

    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private UUID installationId;
    private String subject;
    private String description;
    private String priority;
    private String status;
    private UUID assignedTo;
    private String resolution;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
}
