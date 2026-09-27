package ec.mindalai.managementjsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Cambio de estado de un tenant segun lo expone {@code GET /platform/tenants/{id}/status-history}. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatusHistoryDto {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    private UUID id;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private Instant changedAt;
    private String username;

    /** Fecha y hora del cambio en la zona horaria del servidor de aplicacion. */
    public String changedAtText() {
        return changedAt == null ? "" : FORMATTER.format(changedAt);
    }

    public String transition() {
        String from = fromStatus == null || fromStatus.isBlank() ? "ALTA" : fromStatus;
        return from + " \u2192 " + toStatus;
    }
}
