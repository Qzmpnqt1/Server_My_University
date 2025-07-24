package org.example.service.impl;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.RequiredArgsConstructor;
import org.example.dto.ApiResponse;
import org.example.model.cassandra.*;
import org.example.repository.cassandra.*;
import org.example.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Полная реализация сервиса чатов с использованием Cassandra.
 * Активируется только при использовании профиля "cassandra".
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.cassandra.enabled", havingValue = "true", matchIfMissing = false)
@Profile("cassandra")
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final UserChatRepository userChatRepository;

    @Override
    public ApiResponse<Chat> createChat(UUID userId, List<UUID> participantIds, String chatName, boolean isGroup) {
        try {
            // Create a new chat ID
            UUID chatId = UUID.randomUUID();
            
            // Create chat entry for the creator
            Chat chat = new Chat();
            chat.setKey(new Chat.ChatPrimaryKey(userId, chatId));
            chat.setIsGroup(isGroup);
            chat.setChatName(chatName);
            chat.setCreatedAt(Instant.now());
            
            chatRepository.save(chat);
            
            // Add chat creator as participant and admin
            ChatParticipant creatorParticipant = new ChatParticipant();
            creatorParticipant.setKey(new ChatParticipant.ChatParticipantPrimaryKey(chatId, userId));
            creatorParticipant.setJoinedAt(Instant.now());
            creatorParticipant.setIsAdmin(true);
            
            participantRepository.save(creatorParticipant);
            
            // Initialize creator's unread counter
            UserChat creatorUserChat = new UserChat();
            creatorUserChat.setKey(new UserChat.UserChatPrimaryKey(userId, chatId));
            
            userChatRepository.save(creatorUserChat);
            
            // Add all participants
            for (UUID participantId : participantIds) {
                if (!participantId.equals(userId)) {
                    // Create chat entry for each participant
                    Chat participantChat = new Chat();
                    participantChat.setKey(new Chat.ChatPrimaryKey(participantId, chatId));
                    participantChat.setIsGroup(isGroup);
                    participantChat.setChatName(chatName);
                    participantChat.setCreatedAt(Instant.now());
                    
                    chatRepository.save(participantChat);
                    
                    // Add participant to chat
                    ChatParticipant participant = new ChatParticipant();
                    participant.setKey(new ChatParticipant.ChatParticipantPrimaryKey(chatId, participantId));
                    participant.setJoinedAt(Instant.now());
                    participant.setIsAdmin(false);
                    
                    participantRepository.save(participant);
                    
                    // Initialize participant's unread counter
                    UserChat participantUserChat = new UserChat();
                    participantUserChat.setKey(new UserChat.UserChatPrimaryKey(participantId, chatId));
                    
                    userChatRepository.save(participantUserChat);
                }
            }
            
            return ApiResponse.success("Чат успешно создан", chat);
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при создании чата: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<Chat>> getUserChats(UUID userId) {
        try {
            List<Chat> chats = chatRepository.findByUserId(userId);
            return ApiResponse.success(chats);
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при получении списка чатов: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<Message> sendMessage(UUID chatId, UUID senderId, String content) {
        try {
            // Check if user is participant
            Optional<ChatParticipant> participant = participantRepository.findById(
                    new ChatParticipant.ChatParticipantPrimaryKey(chatId, senderId));
            
            if (participant.isEmpty()) {
                return ApiResponse.error("Пользователь не является участником чата");
            }
            
            // Create and save message
            Message message = new Message();
            message.setKey(new Message.MessagePrimaryKey(chatId, Uuids.timeBased())); // Use time-based UUID
            message.setSenderId(senderId);
            message.setContent(content);
            message.setIsRead(false);
            
            messageRepository.save(message);
            
            // Update unread counters for all participants except sender
            List<ChatParticipant> participants = participantRepository.findByChatId(chatId);
            for (ChatParticipant p : participants) {
                if (!p.getKey().getUserId().equals(senderId)) {
                    userChatRepository.incrementUnreadCount(p.getKey().getUserId(), chatId);
                }
            }
            
            return ApiResponse.success("Сообщение отправлено", message);
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<Message>> getChatMessages(UUID chatId, int limit) {
        try {
            List<Message> messages = messageRepository.findByChatIdWithLimit(chatId, limit);
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при получении сообщений: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<?> markMessagesAsRead(UUID chatId, UUID userId, UUID messageId) {
        try {
            userChatRepository.markAsRead(userId, chatId, messageId);
            return ApiResponse.success("Сообщения отмечены как прочитанные", null);
        } catch (Exception e) {
            return ApiResponse.error("Ошибка при отметке сообщений как прочитанных: " + e.getMessage());
        }
    }
} 