package org.vaadin.marcus.support;

import java.time.LocalDateTime;

public record Ticket(String id, String bookingNumber, String firstName, String lastName,
                     String requestText, TicketStatus status, String proposedAction,
                     String recommendation, LocalDateTime createdAt, LocalDateTime updatedAt) {}
