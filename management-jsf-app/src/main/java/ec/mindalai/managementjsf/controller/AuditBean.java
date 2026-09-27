package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.AuditEntryDto;
import ec.mindalai.managementjsf.dto.PageResult;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Auditoria de plataforma: consulta paginada y filtrada de platform_audit. */
@Named("auditBean")
@ViewScoped
@Getter
@Setter
public class AuditBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    /** Filas por pagina del paginador de la vista, igual que en el resto de paginas. */
    private static final int PAGE_SIZE = 10;

    /**
     * Ventana de eventos que se pide al API. El paginador de la vista reparte esas filas en
     * paginas de {@link #PAGE_SIZE}; el total real del filtro se muestra en la cabecera.
     */
    private static final int FETCH_SIZE = 200;

    @Inject
    private PlatformRestClient restClient;

    private List<AuditEntryDto> entries = new ArrayList<>();
    private long totalElements;
    private int pageSize = PAGE_SIZE;

    private UUID userId;
    private UUID tenantId;
    private String action;
    private String resourceType;
    private String result;
    private LocalDate from;
    private LocalDate to;

    /** Evento abierto en el dialog de detalle. */
    private AuditEntryDto detail;

    /**
     * Aplica los filtros desde la primera pagina. Sin este reset, cambiar un filtro con la
     * vista en la ultima pagina devolvia una tabla vacia sin explicacion.
     */
    public void load() {
        entries = search();
    }

    /**
     * Numero de orden de la fila. {@code rowIndexVar} entrega el indice global de la fila, de
     * modo que la numeracion continua entre paginas sin depender del bean.
     */
    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    public List<AuditEntryDto> search() {
        String cid = correlationId();
        String token = token();
        Map<String, Object> query = new LinkedHashMap<>();
        putIfPresent(query, "userId", userId);
        putIfPresent(query, "tenantId", tenantId);
        putIfPresent(query, "action", trimToNull(action));
        putIfPresent(query, "resourceType", trimToNull(resourceType));
        putIfPresent(query, "result", trimToNull(result));
        putIfPresent(query, "from", atStartOfDay(from));
        putIfPresent(query, "to", atEndOfDay(to));
        query.put("page", 0);
        query.put("size", FETCH_SIZE);
        PageResult<AuditEntryDto> result = call(() -> restClient.audit(token, cid, query), null);
        if (result == null) {
            return entries;
        }
        entries = result.getContent() == null ? new ArrayList<>() : new ArrayList<>(result.getContent());
        totalElements = result.getTotalElements();
        return entries;
    }

    public void reset() {
        userId = null;
        tenantId = null;
        action = null;
        resourceType = null;
        result = null;
        from = null;
        to = null;
        load();
    }

    public void requestDetail(AuditEntryDto entry) {
        if (entry == null) {
            error("Seleccione el evento a revisar");
            return;
        }
        detail = entry;
    }

    public String resultSeverity(String result) {
        return switch (result == null ? "" : result) {
            case "SUCCESS" -> "success";
            case "FAILURE" -> "danger";
            case "DENIED" -> "warning";
            default -> "secondary";
        };
    }

    public long getTotalElements() {
        return totalElements;
    }

    private void putIfPresent(Map<String, Object> query, String key, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            query.put(key, value);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant atStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private Instant atEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
    }
}
