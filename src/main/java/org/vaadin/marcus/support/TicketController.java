package org.vaadin.marcus.support;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService tickets;
    private final String operatorKey;
    public TicketController(TicketService tickets, @Value("${support.operator-key:}") String operatorKey) {
        this.tickets = tickets;
        this.operatorKey = operatorKey;
    }
    public record CreateRequest(String bookingNumber, String firstName, String lastName, String requestText) {}
    @PostMapping
    public Ticket create(@RequestBody CreateRequest request) {
        return tickets.create(request.bookingNumber(), request.firstName(), request.lastName(), request.requestText());
    }
    @GetMapping
    public List<Ticket> list(@RequestHeader(value="X-Operator-Key", required=false) String key) {
        authorize(key); return tickets.list();
    }
    @GetMapping("/{id}")
    public Ticket get(@PathVariable String id, @RequestHeader(value="X-Operator-Key", required=false) String key) {
        authorize(key); return tickets.get(id);
    }
    @GetMapping("/{id}/audit")
    public List<AuditEntry> audit(@PathVariable String id, @RequestHeader(value="X-Operator-Key", required=false) String key) {
        authorize(key); return tickets.audit(id);
    }
    @PostMapping("/{id}/approve")
    public Ticket approve(@PathVariable String id, @RequestHeader(value="X-Operator-Key", required=false) String key) {
        authorize(key); return tickets.approve(id, "operator");
    }
    @PostMapping("/{id}/reject")
    public Ticket reject(@PathVariable String id, @RequestHeader(value="X-Operator-Key", required=false) String key) {
        authorize(key); return tickets.reject(id, "operator");
    }
    private void authorize(String key) {
        if (operatorKey.isBlank() || key == null || !java.security.MessageDigest.isEqual(
                operatorKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator access required");
    }
}
