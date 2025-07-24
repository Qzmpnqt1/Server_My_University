package org.example.model.cassandra;

import lombok.*;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.util.UUID;

@Table("messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @PrimaryKey
    private MessagePrimaryKey key;

    @Column("sender_id")
    private UUID senderId;

    @Column("content")
    private String content;

    @Column("is_read")
    private Boolean isRead;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessagePrimaryKey {
        @PrimaryKeyColumn(name = "chat_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
        private UUID chatId;

        @PrimaryKeyColumn(name = "message_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
        private UUID messageId;
    }
} 