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
import org.primefaces.PrimeFaces;

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

    private static final String TICKET_DIALOG = "ticketDialogWidget";
    private static final String ASSIGN_DIALOG = "assignDialogWidget";
    private static final String RESOLVE_DIALOG = "resolveDialogWidget";
    private static final String CLOSE_DIALOG = "closeDialogWidget";

    @Inject
    private PlatformRestClient restClient;

    private List<SupportTicketDto> tickets = new ArrayList<>();
    private List<TenantDto> tenants = new ArrayList<>();
    private List<InstallationDto> installations = new ArrayList<>();
    private List<PlatformUserDto> operators = new ArrayList<>();

    private UUID tenantFilter;
    private String statusFilter;
    private String priorityFilter;
    private UUID tenantId;
    private UUID installationId;
    private String subject;
    private String description;
    private String priority = "MEDIUM";

    /** Ticket al que se asigna el responsable elegido en el dialog. */
    private SupportTicketDto assigning;
    private UUID assignTo;

    /** Ticket a resolver y texto de resolucion, con el dialog abierto. */
    private SupportTicketDto resolving;
    private String resolution;

    /** Ticket a cerrar, pendiente de la confirmacion en el dialog. */
    private SupportTicketDto closing;

    public int rowNumber(int rowIndex) {
        return rowIndex + 1;
    }

    /**
     * El API no expone filtros de estado ni prioridad para tickets, asi que el recorte es de
     * vista: se aplica sobre la lista ya cargada y por eso tambien es el contenido que se exporta.
     */
    public List<SupportTicketDto> visibleTickets() {
        return tickets.stream()
                .filter(ticket -> statusFilter == null || statusFilter.isBlank()
                        || statusFilter.equals(ticket.getStatus()))
                .filter(ticket -> priorityFilter == null || priorityFilter.isBlank()
                        || priorityFilter.equals(ticket.getPriority()))
                .toList();
    }

    public void openNew() {
        tenantId = tenantFilter;
        installationId = null;
        subject = null;
        description = null;
        priority = "MEDIUM";
    }

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

    public void cancelAssign() {
        assigning = null;
        assignTo = null;
    }

    public void onTenantChange() {
        String cid = correlationId();
        String token = token();
        installations = call(() -> restClient.installations(token, cid, tenantId), installations);
    }

    public void save() {
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
            openNew();
            hideDialog(TICKET_DIALOG);
            load();
        }
    }

    /**
     * Asigna el ticket al operador elegido en el selector. El API expone el UUID del
     * responsable ({@code operatorId}); la vista lo obtiene de {@code GET /users} para que
     * el operador no tenga que escribir el UUID a mano.
     */
    public void requestAssign(SupportTicketDto ticket) {
        if (ticket == null) {
            error("Seleccione el ticket a asignar");
            return;
        }
        assigning = ticket;
        assignTo = ticket.getAssignedTo();
    }

    public void confirmAssign() {
        if (assigning == null) {
            error("Seleccione el ticket a asignar");
            return;
        }
        if (assignTo == null) {
            error("Seleccione el operador responsable");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = assigning.getId();
        UUID operator = assignTo;
        SupportTicketDto assigned = call(() -> restClient.assignTicket(token, cid, id, operator), null);
        if (assigned == null) {
            return;
        }
        success("Ticket asignado a " + operatorName(operator));
        replaceInPlace(assigned);
        assigning = null;
        assignTo = null;
        hideDialog(ASSIGN_DIALOG);
    }

    /** Abre el dialog de resolucion: el API exige el texto de resolucion. */
    public void requestResolve(SupportTicketDto ticket) {
        if (ticket == null) {
            error("Seleccione el ticket a resolver");
            return;
        }
        resolving = ticket;
        resolution = null;
    }

    public void confirmResolve() {
        if (resolving == null) {
            error("Seleccione el ticket a resolver");
            return;
        }
        if (resolution == null || resolution.isBlank()) {
            error("Indique la resolucion del ticket");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = resolving.getId();
        String text = resolution.trim();
        SupportTicketDto resolved = call(() -> restClient.resolveTicket(token, cid, id, text), null);
        if (resolved == null) {
            return;
        }
        success("Ticket resuelto");
        replaceInPlace(resolved);
        resolving = null;
        resolution = null;
        hideDialog(RESOLVE_DIALOG);
    }

    /** Cerrar un ticket no tiene vuelta atras, asi que pide confirmacion antes de llamar al API. */
    public void requestClose(SupportTicketDto ticket) {
        if (ticket == null) {
            error("Seleccione el ticket a cerrar");
            return;
        }
        closing = ticket;
    }

    public void confirmClose() {
        if (closing == null) {
            error("Seleccione el ticket a cerrar");
            return;
        }
        String cid = correlationId();
        String token = token();
        UUID id = closing.getId();
        SupportTicketDto closed = call(() -> restClient.closeTicket(token, cid, id), null);
        if (closed == null) {
            return;
        }
        success("Ticket " + closed.getSubject() + " cerrado");
        replaceInPlace(closed);
        closing = null;
        hideDialog(CLOSE_DIALOG);
    }

    public void cancelClose() {
        closing = null;
    }

    private void replaceInPlace(SupportTicketDto updated) {
        for (int i = 0; i < tickets.size(); i++) {
            if (updated.getId().equals(tickets.get(i).getId())) {
                tickets.set(i, updated);
                return;
            }
        }
        tickets.add(0, updated);
    }

    public String statusSeverity(String status) {
        return switch (status == null ? "" : status) {
            case "OPEN" -> "info";
            case "IN_PROGRESS" -> "warning";
            case "RESOLVED" -> "success";
            default -> "secondary";
        };
    }

    public String prioritySeverity(String priority) {
        return switch (priority == null ? "" : priority) {
            case "CRITICAL" -> "danger";
            case "HIGH" -> "warning";
            case "MEDIUM" -> "info";
            default -> "secondary";
        };
    }

    /** Nombre del responsable asignado, o el guion cuando el ticket sigue sin asignar. */
    public String operatorName(UUID operator) {
        if (operator == null) {
            return "-";
        }
        return operators.stream()
                .filter(candidate -> operator.equals(candidate.getId()))
                .map(PlatformUserDto::displayName)
                .findFirst()
                .orElseGet(() -> operator.toString());
    }

    private void hideDialog(String widgetVar) {
        PrimeFaces.current().executeScript("PF('" + widgetVar + "').hide();");
    }
}
