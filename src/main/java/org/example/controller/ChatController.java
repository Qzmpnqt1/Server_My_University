package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ApiResponse;
import org.example.model.cassandra.Chat;
import org.example.model.cassandra.Message;
import org.example.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Контроллер для API чатов.
 * В текущей версии предоставляет только интерфейсы API, но фактическая
 * функциональность будет реализована позднее с использованием Cassandra.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Chat>> createChat(
            @RequestParam UUID userId,
            @RequestParam List<UUID> participantIds,
            @RequestParam String chatName,
            @RequestParam(defaultValue = "false") boolean isGroup) {
        return ResponseEntity.ok(chatService.createChat(userId, participantIds, chatName, isGroup));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Chat>>> getUserChats(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatService.getUserChats(userId));
    }

    @PostMapping("/message/send")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @RequestParam UUID chatId,
            @RequestParam UUID senderId,
            @RequestParam String content) {
        return ResponseEntity.ok(chatService.sendMessage(chatId, senderId, content));
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getChatMessages(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(chatService.getChatMessages(chatId, limit));
    }

    @PostMapping("/message/read")
    public ResponseEntity<ApiResponse<?>> markMessagesAsRead(
            @RequestParam UUID chatId,
            @RequestParam UUID userId,
            @RequestParam UUID lastReadMessageId) {
        return ResponseEntity.ok(chatService.markMessagesAsRead(chatId, userId, lastReadMessageId));
    }
} 