package org.example.model.cassandra;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.util.UUID;

@Table("user_chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChat {
    @PrimaryKey
    private UserChatPrimaryKey key;

    @Column("last_read_message_id")
    private UUID lastReadMessageId;

    @Column("unread_count")
    private Long unreadCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserChatPrimaryKey {
        @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        private UUID userId;

        @PrimaryKeyColumn(name = "chat_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
        private UUID chatId;
    }
} 