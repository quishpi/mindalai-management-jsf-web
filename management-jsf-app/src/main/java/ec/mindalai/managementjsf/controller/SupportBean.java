package ec.mindalai.managementjsf.controller;

import ec.mindalai.managementjsf.client.PlatformRestClient;
import ec.mindalai.managementjsf.dto.InstallationDto;
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

    private UUID tenantFilter;
    private UUID tenantId;
    private UUID installationId;
    private String subject;
    private String description;
    private String priority = "MEDIUM";
    private String assignTo;

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
        if (tenantId != null) {
            installations = call(() -> restClient.installations(token, cid, tenantId), installations);
        }
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
     * Asigna el ticket a un operador. El API expone solo el UUID del responsable
     * ({@code assignedTo}), por lo que la vista solicita ese identificador en lugar de
     * inventar un mapeo por nombre de usuario.
     */
    public void assign(SupportTicketDto ticket) {
        if (assignTo == null || assignTo.isBlank()) {
            error("Indique el UUID del operador responsable");
            return;
        }
        UUID operator;
        try {
            operator = UUID.fromString(assignTo.trim());
        } catch (IllegalArgumentException ex) {
            error("El UUID del responsable no es valido");
            return;
        }
        String cid = correlationId();
        String token = token();
        SupportTicketDto assigned = call(() -> restClient.assignTicket(token, cid, ticket.getId(), operator), null);
        if (assigned != null) {
            success("Ticket asignado");
            assignTo = null;
            load();
        }
    }

    public void resolve(SupportTicketDto ticket) {
        run(() -> {
            restClient.resolveTicket(token(), correlationId(), ticket.getId());
            success("Ticket resuelto");
            load();
        });
    }

    public void close(SupportTicketDto ticket) {
        run(() -> {
            restClient.closeTicket(token(), correlationId(), ticket.getId());
            success("Ticket cerrado");
            load();
        });
    }
}
