package org.vaadin.marcus.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.vaadin.marcus.service.FlightService;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class TicketServiceTest {
    private TicketService tickets;
    private FlightService flights;

    @BeforeEach void setup() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:test" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        flights = new FlightService();
        tickets = new TicketService(new TicketRepository(new JdbcTemplate(dataSource)), flights);
    }

    @Test void approvalExecutesExactlyOnce() {
        var ticket = tickets.requestCancellation("102", "Jane", "Smith");
        assertEquals(TicketStatus.PENDING_APPROVAL, ticket.status());
        assertEquals("CONFIRMED", flights.getBookingDetails("102", "Jane", "Smith").bookingStatus().name());
        assertEquals(TicketStatus.EXECUTED, tickets.approve(ticket.id(), "operator").status());
        assertEquals(TicketStatus.EXECUTED, tickets.approve(ticket.id(), "operator").status());
        assertEquals("CANCELLED", flights.getBookingDetails("102", "Jane", "Smith").bookingStatus().name());
        assertEquals(1, tickets.audit(ticket.id()).stream().filter(a -> a.action().equals("EXECUTED")).count());
    }

    @Test void rejectionPreventsExecution() {
        var ticket = tickets.requestCancellation("102", "Jane", "Smith");
        assertEquals(TicketStatus.REJECTED, tickets.reject(ticket.id(), "operator").status());
        assertThrows(IllegalStateException.class, () -> tickets.approve(ticket.id(), "operator"));
        assertEquals("CONFIRMED", flights.getBookingDetails("102", "Jane", "Smith").bookingStatus().name());
    }

    @Test void cannotProposeUnsupportedActionOrFakeBooking() {
        assertThrows(IllegalArgumentException.class, () -> tickets.requestCancellation("999", "John", "Doe"));
        var ticket = tickets.create("102", "Jane", "Smith", "Please help");
        assertThrows(IllegalArgumentException.class, () -> tickets.propose(ticket.id(), "REFUND", "Refund"));
        assertEquals(TicketStatus.OPEN, tickets.get(ticket.id()).status());
    }

    @Test void operatorEndpointsRequireValidKey() {
        var controller = new TicketController(tickets, "secret");
        assertThrows(ResponseStatusException.class, () -> controller.list(null));
        assertThrows(ResponseStatusException.class, () -> controller.list("wrong"));
        assertTrue(controller.list("secret").isEmpty());
    }
}
