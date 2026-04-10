package org.example.controller;

import org.example.config.SecurityConfig;
import org.example.exception.GlobalExceptionHandler;
import org.example.service.ChatService;
import org.example.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Tag("security")
@WebMvcTest(controllers = ChatController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChatSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /chats без токена — 401/403")
    void listConversations_anonymous_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/chats"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 401 && sc != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + sc);
                    }
                });
    }

    @Test
    @DisplayName("GET /chats/{id}/messages без токена — 401/403")
    void messages_anonymous_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/chats/11111111-1111-1111-1111-111111111111/messages"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 401 && sc != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + sc);
                    }
                });
    }
}
