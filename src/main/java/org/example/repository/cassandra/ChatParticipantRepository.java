package org.example.repository.cassandra;

import org.example.model.cassandra.ChatParticipant;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends CassandraRepository<ChatParticipant, ChatParticipant.ChatParticipantPrimaryKey> {
    
    @Query("SELECT * FROM chat_participants WHERE chat_id = ?0")
    List<ChatParticipant> findByChatId(UUID chatId);
    
    @Query("SELECT * FROM chat_participants WHERE user_id = ?0 ALLOW FILTERING")
    List<ChatParticipant> findByUserId(UUID userId);
    
    @Query("SELECT * FROM chat_participants WHERE chat_id = ?0 AND is_admin = true ALLOW FILTERING")
    List<ChatParticipant> findAdminsByChatId(UUID chatId);
} 