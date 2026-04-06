package org.example.service;

import org.example.dto.request.SendMessageRequest;
import org.example.dto.response.ChatContactResponse;
import org.example.dto.response.ConversationResponse;
import org.example.dto.response.MessageResponse;

import java.time.Instant;
import java.util.List;

public interface ChatService {

    /**
     * @param universityId для SUPER_ADMIN: null — все активные пользователи; иначе — только вуз.
     *                     для ADMIN игнорируется (всегда кампусный вуз).
     */
    List<ChatContactResponse> listChatContacts(String email, Long universityId);

    List<ConversationResponse> getMyConversations(String email);

    List<MessageResponse> getMessages(String conversationId, String email, int limit, Instant before);

    MessageResponse sendMessage(SendMessageRequest request, String email);

    void markAsRead(String conversationId, String email);
}
