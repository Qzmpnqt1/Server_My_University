package org.example.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String conversationId;
    private Long participantId;
    private String participantName;
    private String lastMessageText;
    private Instant lastMessageAt;
    private Integer unreadCount;
}
