package org.vaadin.marcus.support;

import java.time.LocalDateTime;

public record AuditEntry(long id, String ticketId, String actor, String action,
                         String detail, LocalDateTime createdAt) {}
