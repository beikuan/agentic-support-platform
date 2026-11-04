package org.vaadin.marcus.support;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.marcus.service.FlightService;

@Service
public class TicketService {
    private final TicketRepository repository;
    private final FlightService flights;
    public TicketService(TicketRepository repository, FlightService flights) {
        this.repository = repository;
        this.flights = flights;
    }

    @Transactional
    public Ticket requestCancellation(String bookingNumber, String firstName, String lastName) {
        flights.getBookingDetails(bookingNumber, firstName, lastName);
        String id = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(id, bookingNumber, firstName, lastName,
                "Cancel booking", TicketStatus.OPEN, null, null,
                LocalDateTime.now(), LocalDateTime.now());
        repository.insert(ticket);
        repository.audit(id, "agent", "TOOL_BOOKING_LOOKUP", bookingNumber);
        propose(id, "CANCEL_BOOKING", "Cancellation requested. An operator must approve before execution.");
        return get(id);
    }

    @Transactional
    public Ticket create(String bookingNumber, String firstName, String lastName, String requestText) {
        if (requestText == null || requestText.isBlank() || requestText.length() > 2000) {
            throw new IllegalArgumentException("Request text must contain 1-2000 characters");
        }
        flights.getBookingDetails(bookingNumber, firstName, lastName);
        String id = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(id, bookingNumber, firstName, lastName, requestText,
                TicketStatus.OPEN, null, null, LocalDateTime.now(), LocalDateTime.now());
        repository.insert(ticket);
        repository.audit(id, "customer", "TICKET_CREATED", requestText);
        return get(id);
    }

    @Transactional
    public Ticket propose(String id, String action, String recommendation) {
        if (!"CANCEL_BOOKING".equals(action)) throw new IllegalArgumentException("Unsupported action");
        if (recommendation == null || recommendation.isBlank() || recommendation.length() > 2000)
            throw new IllegalArgumentException("Recommendation must contain 1-2000 characters");
        if (repository.propose(id, action, recommendation) != 1)
            throw new IllegalStateException("Ticket is not open");
        repository.audit(id, "agent", "ACTION_PROPOSED", action);
        return get(id);
    }

    public Ticket get(String id) {
        return repository.find(id).orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }
    public List<Ticket> list() { return repository.list(); }
    public List<AuditEntry> audit(String id) { get(id); return repository.audit(id); }

    @Transactional
    public Ticket reject(String id, String operator) {
        if (repository.transition(id, TicketStatus.PENDING_APPROVAL, TicketStatus.REJECTED) == 1)
            repository.audit(id, operator, "REJECTED", "Operator rejected action");
        Ticket ticket = get(id);
        if (ticket.status() != TicketStatus.REJECTED) throw new IllegalStateException("Ticket cannot be rejected");
        return ticket;
    }

    @Transactional
    public Ticket approve(String id, String operator) {
        if (repository.transition(id, TicketStatus.PENDING_APPROVAL, TicketStatus.APPROVED) == 1)
            repository.audit(id, operator, "APPROVED", "Operator approved action");
        Ticket ticket = get(id);
        if (ticket.status() == TicketStatus.EXECUTED || ticket.status() == TicketStatus.MANUAL_REVIEW)
            return ticket;
        if (ticket.status() != TicketStatus.APPROVED) throw new IllegalStateException("Ticket cannot be approved");
        if (repository.transition(id, TicketStatus.APPROVED, TicketStatus.EXECUTING) != 1) return get(id);
        try {
            flights.cancelBooking(ticket.bookingNumber(), ticket.firstName(), ticket.lastName());
            repository.transition(id, TicketStatus.EXECUTING, TicketStatus.EXECUTED);
            repository.audit(id, operator, "EXECUTED", "Booking cancellation completed");
        } catch (RuntimeException ex) {
            repository.transition(id, TicketStatus.EXECUTING, TicketStatus.MANUAL_REVIEW);
            repository.audit(id, operator, "EXECUTION_FAILED", ex.getClass().getSimpleName());
        }
        return get(id);
    }
}
