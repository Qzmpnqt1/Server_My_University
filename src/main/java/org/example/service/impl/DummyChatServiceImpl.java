package org.example.service.impl;

import org.example.dto.ApiResponse;
import org.example.model.cassandra.Chat;
import org.example.model.cassandra.Message;
import org.example.service.ChatService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Реализация-заглушка для ChatService до момента внедрения Cassandra.
 * Будет использоваться временно, пока не будет реализована полноценная функциональность чатов.
 */
@Service
@Primary
public class DummyChatServiceImpl implements ChatService {

    @Override
    public ApiResponse<Chat> createChat(UUID userId, List<UUID> participantIds, String chatName, boolean isGroup) {
        return ApiResponse.error("Функциональность чатов временно недоступна");
    }

    @Override
    public ApiResponse<List<Chat>> getUserChats(UUID userId) {
        return ApiResponse.error("Функциональность чатов временно недоступна");
    }

    @Override
    public ApiResponse<Message> sendMessage(UUID chatId, UUID senderId, String content) {
        return ApiResponse.error("Функциональность чатов временно недоступна");
    }

    @Override
    public ApiResponse<List<Message>> getChatMessages(UUID chatId, int limit) {
        return ApiResponse.error("Функциональность чатов временно недоступна");
    }

    @Override
    public ApiResponse<?> markMessagesAsRead(UUID chatId, UUID userId, UUID messageId) {
        return ApiResponse.error("Функциональность чатов временно недоступна");
    }
} 