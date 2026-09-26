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
public class TenantDto {

    private UUID id;
    private String legalName;
    private String ruc;
    private String tradeName;
    private String status;
    private String email;
    private String phone;
    private String notes;
    private Instant deactivatedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String displayName() {
        return tradeName == null || tradeName.isBlank() ? legalName : tradeName;
    }
}
