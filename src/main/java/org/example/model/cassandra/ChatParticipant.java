package org.example.model.cassandra;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
import java.util.UUID;

@Table("chat_participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipant {
    @PrimaryKey
    private ChatParticipantPrimaryKey key;

    @Column("joined_at")
    private Instant joinedAt;

    @Column("is_admin")
    private Boolean isAdmin;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatParticipantPrimaryKey {
        @PrimaryKeyColumn(name = "chat_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        private UUID chatId;

        @PrimaryKeyColumn(name = "user_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
        private UUID userId;
    }
} 