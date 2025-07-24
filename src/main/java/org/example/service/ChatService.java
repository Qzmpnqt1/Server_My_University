package org.example.service;

import org.example.dto.ApiResponse;
import org.example.model.cassandra.Chat;
import org.example.model.cassandra.Message;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    
    /**
     * Create a new chat between users
     * 
     * @param userId Owner user ID
     * @param participantIds IDs of participants
     * @param chatName Chat name (optional for group chats)
     * @param isGroup Whether this is a group chat
     * @return API response with the created chat
     */
    ApiResponse<Chat> createChat(UUID userId, List<UUID> participantIds, String chatName, boolean isGroup);
    
    /**
     * Get chats for a user
     * 
     * @param userId User ID
     * @return List of user's chats
     */
    ApiResponse<List<Chat>> getUserChats(UUID userId);
    
    /**
     * Send a message in a chat
     * 
     * @param chatId Chat ID
     * @param senderId Sender ID
     * @param content Message content
     * @return API response with the sent message
     */
    ApiResponse<Message> sendMessage(UUID chatId, UUID senderId, String content);
    
    /**
     * Get messages from a chat
     * 
     * @param chatId Chat ID
     * @param limit Maximum number of messages to retrieve
     * @return List of messages
     */
    ApiResponse<List<Message>> getChatMessages(UUID chatId, int limit);
    
    /**
     * Mark messages as read
     * 
     * @param chatId Chat ID
     * @param userId User ID
     * @param messageId Last read message ID
     * @return API response with success status
     */
    ApiResponse<?> markMessagesAsRead(UUID chatId, UUID userId, UUID messageId);
} 