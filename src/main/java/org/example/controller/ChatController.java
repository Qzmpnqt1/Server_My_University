package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.request.SendMessageRequest;
import org.example.dto.response.ChatContactResponse;
import org.example.dto.response.ConversationResponse;
import org.example.dto.response.MessageResponse;
import org.example.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getMyConversations(Principal principal) {
        return ResponseEntity.ok(chatService.getMyConversations(principal.getName()));
    }

    /** Отдельный путь (не под /users), чтобы не пересекаться с /users/{id}. */
    @GetMapping("/contacts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatContactResponse>> listChatContacts(
            @RequestParam(required = false) Long universityId,
            Principal principal) {
        return ResponseEntity.ok(chatService.listChatContacts(principal.getName(), universityId));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            Principal principal,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Instant before) {
        return ResponseEntity.ok(
                chatService.getMessages(conversationId, principal.getName(), limit, before));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(request, principal.getName()));
    }

    @PatchMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String conversationId,
            Principal principal) {
        chatService.markAsRead(conversationId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
