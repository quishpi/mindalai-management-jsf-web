package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.InstallationDto;
import ec.mindalai.managementjsf.dto.PlatformUserDto;
import ec.mindalai.managementjsf.dto.SupportTicketDto;
import ec.mindalai.managementjsf.dto.TenantDto;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Soporte: ciclo de vida de tickets OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED. */
@Named("supportBean")
@ViewScoped
@Getter
@Setter
public class SupportBean extends AbstractPageBean {

    private static final long serialVersionUID = 1L;

    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
    private static final String[] STATUSES = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"};

    @Inject
    private PlatformRestClient restClient;

    private List<SupportTicketDto> tickets = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<InstallationDto> installations = new ArrayList<>();
    private List<PlatformUserDto> operators = new ArrayList<>();

    private UUID tenantFilter;
    private UUID tenantId;
    private UUID installationId;
    private String subject;
    private String description;
    private String priority = "MEDIUM";
    private UUID assignTo;

    private UUID resolvingId;
    private String resolution;

    public String[] priorities() {
        return PRIORITIES.clone();
    }

    public String[] statuses() {
        return STATUSES.clone();
    }

    public void load() {
        String cid = correlationId();
        String token = token();
        tickets = call(() -> restClient.tickets(token, cid, tenantFilter), tickets);
        tenants = call(() -> restClient.tenants(token, cid, null, null, null), tenants);
        operators = call(() -> restClient.users(token, cid), operators);
        if (tenantId != null) {
            installations = call(() -> restClient.installations(token, cid, tenantId), installations);
        }
    }

    /** Solo operadores activos pueden recibir tickets. */
    public List<PlatformUserDto> activeOperators() {
        return operators.stream()
                .filter(operator -> "ACTIVE".equals(operator.getStatus()))
                .toList();
    }

    public void onTenantChange() {
        String cid = correlationId();
        String token = token();
        installations = call(() -> restClient.installations(token, cid, tenantId), installations);
    }

    public void openTicket() {
        if (tenantId == null || subject == null || subject.isBlank() || description == null || description.isBlank()) {
            error("Complete tenant, asunto y descripcion");
            return;
        }
        String cid = correlationId();
        String token = token();
        String ticketSubject = subject.trim();
        String ticketDescription = description.trim();
        String ticketPriority = priority == null || priority.isBlank() ? "MEDIUM" : priority;
        SupportTicketDto created = call(() -> restClient.openTicket(token, cid, tenantId, installationId,
                ticketSubject, ticketDescription, ticketPriority), null);
        if (created != null) {
            success("Ticket abierto");
            subject = null;
            description = null;
            load();
        }
    }

    /**
     * Asigna el ticket al operador elegido en el selector. El API expone el UUID del
     * responsable ({@code operatorId}); la vista lo obtiene de {@code GET /users} para que
     * el operador no tenga que escribir el UUID a mano.
     */
    public void assign(SupportTicketDto ticket) {
        if (assignTo == null) {
            error("Seleccione el operador responsable");
            return;
        }
        String cid = correlationId();
        String token = token();
        SupportTicketDto assigned = call(() -> restClient.assignTicket(token, cid, ticket.getId(), assignTo), null);
        if (assigned != null) {
            success("Ticket asignado");
            assignTo = null;
            load();
        }
    }

    /** Abre la confirmacion de resolucion: el API exige el texto de resolucion. */
    public void prepareResolve(SupportTicketDto ticket) {
        resolvingId = ticket.getId();
        resolution = null;
    }

    public void cancelResolve() {
        resolvingId = null;
        resolution = null;
    }

    public void resolve() {
        if (resolvingId == null) {
            error("Seleccione el ticket a resolver");
            return;
        }
        if (resolution == null || resolution.isBlank()) {
            error("Indique la resolucion del ticket");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = resolvingId;
        String text = resolution.trim();
        SupportTicketDto resolved = call(() -> restClient.resolveTicket(token, cid, id, text), null);
        if (resolved != null) {
            success("Ticket resuelto");
            cancelResolve();
            load();
        }
    }

    public void close(SupportTicketDto ticket) {
        run(() -> {
            restClient.closeTicket(token(), correlationId(), ticket.getId());
            success("Ticket cerrado");
            load();
        });
    }
}
