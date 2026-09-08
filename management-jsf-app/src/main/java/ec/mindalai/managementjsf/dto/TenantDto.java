package ec.mindalai.managementjsf.dto;

import lombok.*; import java.util.UUID; import java.time.Instant;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenantDto {
    private UUID id;
    private String legalName;
    private String ruc;
    private String tradeName;
    private String status;
    private Instant createdAt;
}
