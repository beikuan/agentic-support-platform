package org.vaadin.marcus.langchain4j;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.vaadin.marcus.service.BookingDetails;
import org.vaadin.marcus.service.FlightService;
import org.vaadin.marcus.support.TicketService;

import java.time.LocalDate;

@Component
public class LangChain4jTools {

    private final FlightService service;
    private final TicketService tickets;

    public LangChain4jTools(FlightService service, TicketService tickets) {
        this.service = service;
        this.tickets = tickets;
    }

    @Tool("""
            Retrieves information about an existing booking,
            such as the flight date, booking status, departure and arrival airports, and booking class.
            """)
    public BookingDetails getBookingDetails(String bookingNumber, String firstName, String lastName) {
        return service.getBookingDetails(bookingNumber, firstName, lastName);
    }

    @Tool("Request cancellation of a booking. Returns a ticket ID; cancellation requires human approval and is not performed by this tool.")
    public String requestCancellation(String bookingNumber, String firstName, String lastName) {
        return tickets.requestCancellation(bookingNumber, firstName, lastName).id();
    }

}
