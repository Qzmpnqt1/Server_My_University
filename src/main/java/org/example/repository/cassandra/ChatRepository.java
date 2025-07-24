package org.example.repository.cassandra;

import org.example.model.cassandra.Chat;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends CassandraRepository<Chat, Chat.ChatPrimaryKey> {
    
    @Query("SELECT * FROM chats WHERE user_id = ?0 ALLOW FILTERING")
    List<Chat> findByUserId(UUID userId);
    
    @Query("SELECT * FROM chats WHERE chat_id = ?0 ALLOW FILTERING")
    List<Chat> findByChatId(UUID chatId);
} 