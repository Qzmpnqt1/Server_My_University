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
import java.util.UUID;

@Repository
public class CassandraMessageRepository {

    private final CqlSession cqlSession;

    private PreparedStatement selectMessages;
    private PreparedStatement selectMessagesBefore;
    private PreparedStatement insertMessage;

    @Autowired
    public CassandraMessageRepository(@Autowired(required = false) CqlSession cqlSession) {
        this.cqlSession = cqlSession;
    }

    @PostConstruct
    public void init() {
        if (cqlSession == null) return;

        selectMessages = cqlSession.prepare(
                "SELECT message_id, sender_id, sender_name, text, sent_at " +
                "FROM messages_by_conversation " +
                "WHERE conversation_id = ? LIMIT ?");

        selectMessagesBefore = cqlSession.prepare(
                "SELECT message_id, sender_id, sender_name, text, sent_at " +
                "FROM messages_by_conversation " +
                "WHERE conversation_id = ? AND sent_at < ? LIMIT ?");

        insertMessage = cqlSession.prepare(
                "INSERT INTO messages_by_conversation " +
                "(conversation_id, message_id, sender_id, sender_name, text, sent_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)");
    }

    public List<Row> findByConversationId(UUID conversationId, int limit) {
        ResultSet rs = cqlSession.execute(selectMessages.bind(conversationId, limit));
        List<Row> rows = new ArrayList<>();
        for (Row row : rs) rows.add(row);
        return rows;
    }

    public List<Row> findByConversationIdBefore(UUID conversationId, Instant before, int limit) {
        ResultSet rs = cqlSession.execute(selectMessagesBefore.bind(conversationId, before, limit));
        List<Row> rows = new ArrayList<>();
        for (Row row : rs) rows.add(row);
        return rows;
    }

    public void insert(UUID conversationId, UUID messageId, Long senderId,
                       String senderName, String text, Instant sentAt) {
        cqlSession.execute(insertMessage.bind(conversationId, messageId, senderId, senderName, text, sentAt));
    }

    public boolean isAvailable() {
        return cqlSession != null;
    }
}
