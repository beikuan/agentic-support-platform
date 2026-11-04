package org.vaadin.marcus.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepository {
    private final JdbcTemplate jdbc;
    public TicketRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void insert(Ticket t) {
        jdbc.update("INSERT INTO support_ticket(id,booking_number,first_name,last_name,request_text,status,proposed_action,recommendation) VALUES (?,?,?,?,?,?,?,?)",
                t.id(), t.bookingNumber(), t.firstName(), t.lastName(), t.requestText(),
                t.status().name(), t.proposedAction(), t.recommendation());
    }
    public Optional<Ticket> find(String id) {
        return jdbc.query("SELECT * FROM support_ticket WHERE id=?", this::map, id).stream().findFirst();
    }
    public List<Ticket> list() {
        return jdbc.query("SELECT * FROM support_ticket ORDER BY created_at DESC", this::map);
    }
    public int transition(String id, TicketStatus expected, TicketStatus next) {
        return jdbc.update("UPDATE support_ticket SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND status=?",
                next.name(), id, expected.name());
    }
    public int propose(String id, String action, String recommendation) {
        return jdbc.update("UPDATE support_ticket SET status=?,proposed_action=?,recommendation=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status=?",
                TicketStatus.PENDING_APPROVAL.name(), action, recommendation, id, TicketStatus.OPEN.name());
    }
    public void audit(String id, String actor, String action, String detail) {
        jdbc.update("INSERT INTO support_audit(ticket_id,actor,action,detail) VALUES (?,?,?,?)", id, actor, action, detail);
    }
    public List<AuditEntry> audit(String id) {
        return jdbc.query("SELECT * FROM support_audit WHERE ticket_id=? ORDER BY id", (rs,n) ->
                new AuditEntry(rs.getLong("id"), rs.getString("ticket_id"), rs.getString("actor"),
                        rs.getString("action"), rs.getString("detail"), rs.getTimestamp("created_at").toLocalDateTime()), id);
    }
    private Ticket map(ResultSet rs, int n) throws SQLException {
        return new Ticket(rs.getString("id"), rs.getString("booking_number"), rs.getString("first_name"),
                rs.getString("last_name"), rs.getString("request_text"), TicketStatus.valueOf(rs.getString("status")),
                rs.getString("proposed_action"), rs.getString("recommendation"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }
}
