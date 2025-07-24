package org.example.repository.cassandra;

import org.example.model.cassandra.Message;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends CassandraRepository<Message, Message.MessagePrimaryKey> {
    
    @Query("SELECT * FROM messages WHERE chat_id = ?0 ORDER BY message_id DESC LIMIT ?1")
    List<Message> findByChatIdWithLimit(UUID chatId, int limit);
    
    @Query("SELECT * FROM messages WHERE sender_id = ?0 ALLOW FILTERING")
    List<Message> findBySenderId(UUID senderId);
} 