package org.example.repository.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CassandraConversationRepository {

    private final CqlSession cqlSession;

    private PreparedStatement selectByUser;
    private PreparedStatement insertConversation;
    private PreparedStatement deleteConversation;
    private PreparedStatement updateUnreadCount;

    @Autowired
    public CassandraConversationRepository(@Autowired(required = false) CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @PostConstruct
    public void init() {
        if (cqlSession == null) return;

        selectByUser = cqlSession.prepare(
                "SELECT conversation_id, participant_id, participant_name, " +
                "last_message_text, last_message_at, unread_count " +
                "FROM conversations_by_user WHERE user_id = ?");

        insertConversation = cqlSession.prepare(
                "INSERT INTO conversations_by_user " +
                "(user_id, conversation_id, participant_id, participant_name, " +
                "last_message_text, last_message_at, unread_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)");

        deleteConversation = cqlSession.prepare(
                "DELETE FROM conversations_by_user " +
                "WHERE user_id = ? AND last_message_at = ? AND conversation_id = ?");

        updateUnreadCount = cqlSession.prepare(
                "UPDATE conversations_by_user SET unread_count = ? " +
                "WHERE user_id = ? AND last_message_at = ? AND conversation_id = ?");
    }

    public List<Row> findByUserId(Long userId) {
        ResultSet rs = cqlSession.execute(selectByUser.bind(userId));
        List<Row> rows = new ArrayList<>();
        for (Row row : rs) rows.add(row);
        return rows;
    }

    public Optional<Row> findConversationEntry(Long userId, UUID conversationId) {
        for (Row row : findByUserId(userId)) {
            if (conversationId.equals(row.getUuid("conversation_id"))) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public void upsert(Long userId, UUID conversationId, Long participantId,
                       String participantName, String lastText, Instant lastAt, int unread) {
        cqlSession.execute(insertConversation.bind(
                userId, conversationId, participantId, participantName, lastText, lastAt, unread));
    }

    public void deleteEntry(Long userId, Instant lastMessageAt, UUID conversationId) {
        cqlSession.execute(deleteConversation.bind(userId, lastMessageAt, conversationId));
    }

    public void deleteOldAndInsertNew(Long userId, UUID conversationId, Long participantId,
                                      String participantName, String lastText, Instant newAt, int unread) {
        findConversationEntry(userId, conversationId).ifPresent(row ->
                deleteEntry(userId, row.getInstant("last_message_at"), conversationId));
        upsert(userId, conversationId, participantId, participantName, lastText, newAt, unread);
    }

    public void markAsRead(Long userId, UUID conversationId) {
        findConversationEntry(userId, conversationId).ifPresent(row -> {
            Instant ts = row.getInstant("last_message_at");
            cqlSession.execute(updateUnreadCount.bind(0, userId, ts, conversationId));
        });
    }

    public boolean isAvailable() {
        return cqlSession != null;
    }
}
