package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.SecurityConfig;
import org.example.dto.request.SendMessageRequest;
import org.example.dto.response.ChatContactResponse;
import org.example.dto.response.ConversationResponse;
import org.example.dto.response.MessageResponse;
import org.example.exception.BadRequestException;
import org.example.exception.GlobalExceptionHandler;
import org.example.exception.ResourceNotFoundException;
import org.example.service.ChatService;
import org.example.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ChatService chatService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;

    private void mockAuth(String token, String email, String role) {
        User user = new User(email, "hash", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(eq(token), any())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
    }

    private static final String CONV_ID = "d3a1f4c8-9b2e-3d7a-8f1e-6b5c4a3d2e1f";
    private static final Instant NOW = Instant.parse("2025-09-01T10:00:00Z");

    private ConversationResponse sampleConversation() {
        return ConversationResponse.builder()
                .conversationId(CONV_ID)
                .participantId(3L)
                .participantName("Петрова Анна")
                .lastMessageText("Привет")
                .lastMessageAt(NOW)
                .unreadCount(1)
                .build();
    }

    private MessageResponse sampleMessage() {
        return MessageResponse.builder()
                .messageId("msg-1")
                .conversationId(CONV_ID)
                .senderId(2L)
                .senderName("Иванов Пётр")
                .text("Привет!")
                .sentAt(NOW)
                .build();
    }

    // ── GET /chats/contacts ───────────────────────────────────────

    @Test
    @DisplayName("GET /chats/contacts — список контактов для нового чата")
    void getContacts_authenticated_200() throws Exception {
        mockAuth("token", "admin@uni.ru", "ADMIN");
        when(chatService.listChatContacts(eq("admin@uni.ru"), isNull())).thenReturn(List.of(
                ChatContactResponse.builder()
                        .id(2L).email("u@test.ru").firstName("Иван").lastName("Иванов")
                        .userType("STUDENT")
                        .build()));

        mockMvc.perform(get("/api/v1/chats/contacts")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("u@test.ru"));

        verify(chatService).listChatContacts(eq("admin@uni.ru"), isNull());
    }

    // ── GET /chats — get my conversations ─────────────────────────

    @Test
    @DisplayName("GET /chats — student sees own conversations")
    void getConversations_student_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        when(chatService.getMyConversations("student@uni.ru"))
                .thenReturn(List.of(sampleConversation()));

        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conversationId").value(CONV_ID))
                .andExpect(jsonPath("$[0].participantName").value("Петрова Анна"))
                .andExpect(jsonPath("$[0].unreadCount").value(1));
    }

    @Test
    @DisplayName("GET /chats — teacher sees own conversations")
    void getConversations_teacher_200() throws Exception {
        mockAuth("t-token", "teacher@uni.ru", "TEACHER");
        when(chatService.getMyConversations("teacher@uni.ru"))
                .thenReturn(List.of(sampleConversation()));

        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer t-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conversationId").value(CONV_ID));
    }

    @Test
    @DisplayName("GET /chats — admin sees own conversations")
    void getConversations_admin_200() throws Exception {
        mockAuth("a-token", "admin@uni.ru", "ADMIN");
        when(chatService.getMyConversations("admin@uni.ru"))
                .thenReturn(List.of(sampleConversation()));

        mockMvc.perform(get("/api/v1/chats")
                        .header("Authorization", "Bearer a-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /chats — unauthenticated returns 403")
    void getConversations_unauth_403() throws Exception {
        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(status().isForbidden());
    }

    // ── GET /chats/{id}/messages — get dialog messages ────────────

    @Test
    @DisplayName("GET /chats/{id}/messages — returns messages with default limit")
    void getMessages_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        when(chatService.getMessages(eq(CONV_ID), eq("student@uni.ru"), eq(50), isNull()))
                .thenReturn(List.of(sampleMessage()));

        mockMvc.perform(get("/api/v1/chats/" + CONV_ID + "/messages")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Привет!"))
                .andExpect(jsonPath("$[0].senderId").value(2))
                .andExpect(jsonPath("$[0].senderName").value("Иванов Пётр"));
    }

    @Test
    @DisplayName("GET /chats/{id}/messages — with custom limit and before")
    void getMessages_withParams_200() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        Instant before = Instant.parse("2025-09-01T12:00:00Z");
        when(chatService.getMessages(eq(CONV_ID), eq("student@uni.ru"), eq(20), eq(before)))
                .thenReturn(List.of(sampleMessage()));

        mockMvc.perform(get("/api/v1/chats/" + CONV_ID + "/messages")
                        .header("Authorization", "Bearer s-token")
                        .param("limit", "20")
                        .param("before", before.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Привет!"));
    }

    @Test
    @DisplayName("GET /chats/{id}/messages — non-participant gets 400")
    void getMessages_notParticipant_400() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        when(chatService.getMessages(eq(CONV_ID), eq("student@uni.ru"), anyInt(), any()))
                .thenThrow(new BadRequestException("У вас нет доступа к этой беседе"));

        mockMvc.perform(get("/api/v1/chats/" + CONV_ID + "/messages")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("У вас нет доступа к этой беседе"));
    }

    @Test
    @DisplayName("GET /chats/{id}/messages — unauthenticated returns 403")
    void getMessages_unauth_403() throws Exception {
        mockMvc.perform(get("/api/v1/chats/" + CONV_ID + "/messages"))
                .andExpect(status().isForbidden());
    }

    // ── POST /chats/messages — send message ───────────────────────

    @Test
    @DisplayName("POST /chats/messages — sends message successfully")
    void sendMessage_201() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        SendMessageRequest req = SendMessageRequest.builder()
                .recipientId(3L).text("Привет!").build();
        when(chatService.sendMessage(any(SendMessageRequest.class), eq("student@uni.ru")))
                .thenReturn(sampleMessage());

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Привет!"))
                .andExpect(jsonPath("$.conversationId").value(CONV_ID));
    }

    @Test
    @DisplayName("POST /chats/messages — validation error on blank text")
    void sendMessage_blankText_400() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        SendMessageRequest req = SendMessageRequest.builder()
                .recipientId(3L).text("").build();

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chats/messages — validation error on null recipientId")
    void sendMessage_nullRecipient_400() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        SendMessageRequest req = SendMessageRequest.builder()
                .text("Привет!").build();

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /chats/messages — self-message returns 400")
    void sendMessage_self_400() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        SendMessageRequest req = SendMessageRequest.builder()
                .recipientId(2L).text("Себе").build();
        when(chatService.sendMessage(any(SendMessageRequest.class), eq("student@uni.ru")))
                .thenThrow(new BadRequestException("Нельзя отправить сообщение самому себе"));

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Нельзя отправить сообщение самому себе"));
    }

    @Test
    @DisplayName("POST /chats/messages — recipient not found returns 404")
    void sendMessage_recipientNotFound_404() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        SendMessageRequest req = SendMessageRequest.builder()
                .recipientId(999L).text("Привет!").build();
        when(chatService.sendMessage(any(SendMessageRequest.class), eq("student@uni.ru")))
                .thenThrow(new ResourceNotFoundException("Получатель не найден"));

        mockMvc.perform(post("/api/v1/chats/messages")
                        .header("Authorization", "Bearer s-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Получатель не найден"));
    }

    @Test
    @DisplayName("POST /chats/messages — unauthenticated returns 403")
    void sendMessage_unauth_403() throws Exception {
        SendMessageRequest req = SendMessageRequest.builder()
                .recipientId(3L).text("Привет!").build();

        mockMvc.perform(post("/api/v1/chats/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /chats/{id}/read — mark as read ─────────────────────

    @Test
    @DisplayName("PATCH /chats/{id}/read — marks as read")
    void markAsRead_204() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        doNothing().when(chatService).markAsRead(CONV_ID, "student@uni.ru");

        mockMvc.perform(patch("/api/v1/chats/" + CONV_ID + "/read")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isNoContent());

        verify(chatService).markAsRead(CONV_ID, "student@uni.ru");
    }

    @Test
    @DisplayName("PATCH /chats/{id}/read — not participant returns 400")
    void markAsRead_notParticipant_400() throws Exception {
        mockAuth("s-token", "student@uni.ru", "STUDENT");
        doThrow(new BadRequestException("У вас нет доступа к этой беседе"))
                .when(chatService).markAsRead(CONV_ID, "student@uni.ru");

        mockMvc.perform(patch("/api/v1/chats/" + CONV_ID + "/read")
                        .header("Authorization", "Bearer s-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /chats/{id}/read — unauthenticated returns 403")
    void markAsRead_unauth_403() throws Exception {
        mockMvc.perform(patch("/api/v1/chats/" + CONV_ID + "/read"))
                .andExpect(status().isForbidden());
    }

    // ── RBAC — all roles can use chat ─────────────────────────────

    @Test
    @DisplayName("All roles (STUDENT, TEACHER, ADMIN) can access chat endpoints")
    void allRolesCanChat() throws Exception {
        for (String role : List.of("STUDENT", "TEACHER", "ADMIN")) {
            String token = role.toLowerCase() + "-token";
            String email = role.toLowerCase() + "@uni.ru";
            mockAuth(token, email, role);
            when(chatService.getMyConversations(email)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/chats")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}
