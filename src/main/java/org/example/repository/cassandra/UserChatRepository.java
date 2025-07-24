package org.example.repository.cassandra;

import org.example.model.cassandra.UserChat;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserChatRepository extends CassandraRepository<UserChat, UserChat.UserChatPrimaryKey> {
    
    @Query("SELECT * FROM user_chats WHERE user_id = ?0")
    List<UserChat> findByUserId(UUID userId);
    
    @Query("UPDATE user_chats SET unread_count = unread_count + 1 WHERE user_id = ?0 AND chat_id = ?1")
    void incrementUnreadCount(UUID userId, UUID chatId);
    
    @Query("UPDATE user_chats SET unread_count = 0, last_read_message_id = ?2 WHERE user_id = ?0 AND chat_id = ?1")
    void markAsRead(UUID userId, UUID chatId, UUID lastReadMessageId);
} 