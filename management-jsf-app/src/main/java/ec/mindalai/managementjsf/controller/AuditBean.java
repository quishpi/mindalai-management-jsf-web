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

    private static final int PAGE_SIZE = 20;

    @Inject
    private PlatformRestClient restClient;

    private List<AuditEntryDto> entries = new ArrayList<>();
    private long totalElements;
    private int totalPages;
    private int page;

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
        page = 0;
        entries = search();
    }

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
        query.put("page", page);
        query.put("size", PAGE_SIZE);
        PageResult<AuditEntryDto> result = call(() -> restClient.audit(token, cid, query), null);
        if (result == null) {
            return entries;
        }
        entries = result.getContent() == null ? new ArrayList<>() : new ArrayList<>(result.getContent());
        totalElements = result.getTotalElements();
        totalPages = result.getTotalPages();
        return entries;
    }

    public void nextPage() {
        if (page + 1 < totalPages) {
            page++;
            load();
        }
    }

    public void previousPage() {
        if (page > 0) {
            page--;
            load();
        }
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

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public boolean isHasPrevious() {
        return page > 0;
    }

    public boolean isHasNext() {
        return page + 1 < totalPages;
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
