package org.example.service;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.example.dto.request.SendMessageRequest;
import org.example.dto.response.ConversationResponse;
import org.example.dto.response.MessageResponse;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.StudentProfileRepository;
import org.example.repository.UsersRepository;
import org.example.repository.cassandra.CassandraConversationRepository;
import org.example.repository.cassandra.CassandraMessageRepository;
import org.example.service.impl.ChatServiceImpl;
import org.example.service.UniversityScopeService;
import org.example.service.ViewerUniversityResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private UsersRepository usersRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private CassandraConversationRepository conversationRepo;
    @Mock private CassandraMessageRepository messageRepo;
    @Mock private NotificationService notificationService;
    @Mock private UniversityScopeService universityScopeService;
    @Mock private ViewerUniversityResolver viewerUniversityResolver;

    private ChatServiceImpl chatService;

    private Users student;
    private Users teacher;

    private static final UUID CONV_ID = ChatServiceImpl.generateConversationId(2L, 3L);
    private static final Instant NOW = Instant.parse("2025-09-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        lenient().doNothing().when(notificationService).notifyNewChatMessage(anyLong(), anyString());
        chatService = new ChatServiceImpl(usersRepository, studentProfileRepository,
                conversationRepo, messageRepo, notificationService, universityScopeService,
                viewerUniversityResolver);

        student = Users.builder()
                .id(2L).email("student@test.ru").firstName("Пётр").lastName("Иванов")
                .userType(UserType.STUDENT).isActive(true).build();

        teacher = Users.builder()
                .id(3L).email("teacher@test.ru").firstName("Анна").lastName("Петрова")
                .userType(UserType.TEACHER).isActive(true).build();
    }

    private Row mockConversationRow(UUID convId, Long participantId, String name,
                                     String lastText, Instant lastAt, int unread) {
        Row row = mock(Row.class);
        lenient().when(row.getUuid("conversation_id")).thenReturn(convId);
        lenient().when(row.getLong("participant_id")).thenReturn(participantId);
        lenient().when(row.getString("participant_name")).thenReturn(name);
        lenient().when(row.getString("last_message_text")).thenReturn(lastText);
        lenient().when(row.getInstant("last_message_at")).thenReturn(lastAt);
        lenient().when(row.getInt("unread_count")).thenReturn(unread);
        return row;
    }

    private Row mockMessageRow(UUID msgId, Long senderId, String senderName,
                                String text, Instant sentAt) {
        Row row = mock(Row.class);
        when(row.getUuid("message_id")).thenReturn(msgId);
        when(row.getLong("sender_id")).thenReturn(senderId);
        when(row.getString("sender_name")).thenReturn(senderName);
        when(row.getString("text")).thenReturn(text);
        when(row.getInstant("sent_at")).thenReturn(sentAt);
        return row;
    }

    // ── Cassandra unavailable ─────────────────────────────────────

    @Test
    @DisplayName("All methods throw when Cassandra is unavailable")
    void cassandraUnavailable_throwsBadRequest() {
        when(conversationRepo.isAvailable()).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> chatService.getMyConversations("a@test.ru"));
        assertThrows(BadRequestException.class,
                () -> chatService.getMessages("id", "a@test.ru", 50, null));
        assertThrows(BadRequestException.class,
                () -> chatService.sendMessage(new SendMessageRequest(), "a@test.ru"));
        assertThrows(BadRequestException.class,
                () -> chatService.markAsRead("id", "a@test.ru"));
    }

    // ── getMyConversations ────────────────────────────────────────

    @Nested
    @DisplayName("getMyConversations")
    class GetMyConversations {

        @Test
        @DisplayName("returns list of conversations for user")
        void returnsConversations() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            Row row = mockConversationRow(CONV_ID, 3L, "Петрова Анна", "Привет", NOW, 1);
            when(conversationRepo.findByUserId(2L)).thenReturn(List.of(row));

            List<ConversationResponse> result = chatService.getMyConversations("student@test.ru");

            assertEquals(1, result.size());
            assertEquals("Петрова Анна", result.get(0).getParticipantName());
            assertEquals(1, result.get(0).getUnreadCount());
            assertEquals(CONV_ID.toString(), result.get(0).getConversationId());
        }

        @Test
        @DisplayName("returns empty list when no conversations")
        void returnsEmpty() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
            when(conversationRepo.findByUserId(2L)).thenReturn(Collections.emptyList());

            List<ConversationResponse> result = chatService.getMyConversations("student@test.ru");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("throws when user not found")
        void userNotFound() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("unknown@test.ru")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> chatService.getMyConversations("unknown@test.ru"));
        }
    }

    // ── getMessages ───────────────────────────────────────────────

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("returns paginated messages for participant")
        void returnsMessages() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            Row convRow = mockConversationRow(CONV_ID, 3L, "Петрова Анна", "text", NOW, 0);
            when(conversationRepo.findConversationEntry(2L, CONV_ID)).thenReturn(Optional.of(convRow));

            UUID msgId = Uuids.timeBased();
            Row msgRow = mockMessageRow(msgId, 2L, "Иванов Пётр", "Привет", NOW);
            when(messageRepo.findByConversationId(CONV_ID, 50)).thenReturn(List.of(msgRow));

            List<MessageResponse> result = chatService.getMessages(
                    CONV_ID.toString(), "student@test.ru", 50, null);

            assertEquals(1, result.size());
            assertEquals("Привет", result.get(0).getText());
            assertEquals(2L, result.get(0).getSenderId());
        }

        @Test
        @DisplayName("returns messages before cursor")
        void returnsMessagesBefore() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            Row convRow = mockConversationRow(CONV_ID, 3L, "Петрова Анна", "text", NOW, 0);
            when(conversationRepo.findConversationEntry(2L, CONV_ID)).thenReturn(Optional.of(convRow));

            Instant before = Instant.parse("2025-09-01T10:05:00Z");
            UUID msgId = Uuids.timeBased();
            Row msgRow = mockMessageRow(msgId, 3L, "Петрова Анна", "Ранее", NOW);
            when(messageRepo.findByConversationIdBefore(CONV_ID, before, 50)).thenReturn(List.of(msgRow));

            List<MessageResponse> result = chatService.getMessages(
                    CONV_ID.toString(), "student@test.ru", 50, before);

            assertEquals(1, result.size());
            assertEquals("Ранее", result.get(0).getText());
        }

        @Test
        @DisplayName("throws when user is not a participant")
        void notParticipant() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
            when(conversationRepo.findConversationEntry(2L, CONV_ID)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> chatService.getMessages(CONV_ID.toString(), "student@test.ru", 50, null));
        }

        @Test
        @DisplayName("throws on invalid conversation id format")
        void invalidConvId() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            assertThrows(BadRequestException.class,
                    () -> chatService.getMessages("not-a-uuid", "student@test.ru", 50, null));
        }

        @Test
        @DisplayName("limit is clamped to 1..200")
        void limitClamped() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            Row convRow = mockConversationRow(CONV_ID, 3L, "Петрова Анна", "text", NOW, 0);
            when(conversationRepo.findConversationEntry(2L, CONV_ID)).thenReturn(Optional.of(convRow));
            when(messageRepo.findByConversationId(eq(CONV_ID), anyInt())).thenReturn(Collections.emptyList());

            chatService.getMessages(CONV_ID.toString(), "student@test.ru", 500, null);
            verify(messageRepo).findByConversationId(CONV_ID, 200);

            chatService.getMessages(CONV_ID.toString(), "student@test.ru", -5, null);
            verify(messageRepo).findByConversationId(CONV_ID, 1);
        }
    }

    // ── sendMessage ───────────────────────────────────────────────

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("sends message successfully and returns response")
        void success() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
            when(usersRepository.findById(3L)).thenReturn(Optional.of(teacher));
            University uni = University.builder().id(1L).name("МГУ").build();
            Institute inst = Institute.builder().id(1L).name("ИТИ").university(uni).build();
            AcademicGroup group = AcademicGroup.builder().id(1L).name("ИТ-101").build();
            StudentProfile sp = StudentProfile.builder().id(1L).user(student).group(group).institute(inst).build();
            when(studentProfileRepository.findFetchedByUserId(2L)).thenReturn(Optional.of(sp));
            when(universityScopeService.userBelongsToUniversity(3L, 1L)).thenReturn(true);

            SendMessageRequest req = SendMessageRequest.builder()
                    .recipientId(3L).text("Привет!").build();

            MessageResponse result = chatService.sendMessage(req, "student@test.ru");

            assertNotNull(result.getMessageId());
            assertNotNull(result.getConversationId());
            assertEquals(2L, result.getSenderId());
            assertEquals("Иванов Пётр", result.getSenderName());
            assertEquals("Привет!", result.getText());
            assertNotNull(result.getSentAt());

            verify(messageRepo).insert(eq(CONV_ID), any(UUID.class), eq(2L),
                    eq("Иванов Пётр"), eq("Привет!"), any(Instant.class));
            verify(conversationRepo).deleteOldAndInsertNew(
                    eq(2L), eq(CONV_ID), eq(3L), eq("Петрова Анна"),
                    eq("Привет!"), any(Instant.class), eq(0));
            verify(conversationRepo).deleteOldAndInsertNew(
                    eq(3L), eq(CONV_ID), eq(2L), eq("Иванов Пётр"),
                    eq("Привет!"), any(Instant.class), eq(1));
        }

        @Test
        @DisplayName("throws when sending message to self")
        void selfMessage() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
            when(usersRepository.findById(2L)).thenReturn(Optional.of(student));

            SendMessageRequest req = SendMessageRequest.builder()
                    .recipientId(2L).text("Себе").build();

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> chatService.sendMessage(req, "student@test.ru"));
            assertTrue(ex.getMessage().contains("самому себе"));
        }

        @Test
        @DisplayName("throws when recipient not found")
        void recipientNotFound() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
            when(usersRepository.findById(999L)).thenReturn(Optional.empty());

            SendMessageRequest req = SendMessageRequest.builder()
                    .recipientId(999L).text("Привет!").build();

            assertThrows(ResourceNotFoundException.class,
                    () -> chatService.sendMessage(req, "student@test.ru"));
        }

        @Test
        @DisplayName("throws when sender not found")
        void senderNotFound() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("unknown@test.ru")).thenReturn(Optional.empty());

            SendMessageRequest req = SendMessageRequest.builder()
                    .recipientId(3L).text("Привет!").build();

            assertThrows(ResourceNotFoundException.class,
                    () -> chatService.sendMessage(req, "unknown@test.ru"));
        }

        @Test
        @DisplayName("conversation ID is deterministic and symmetric")
        void conversationIdDeterministic() {
            UUID id1 = ChatServiceImpl.generateConversationId(2L, 3L);
            UUID id2 = ChatServiceImpl.generateConversationId(3L, 2L);
            assertEquals(id1, id2);
        }
    }

    // ── markAsRead ────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("marks conversation as read for participant")
        void success() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            Row convRow = mockConversationRow(CONV_ID, 3L, "Петрова Анна", "text", NOW, 5);
            when(conversationRepo.findConversationEntry(2L, CONV_ID)).thenReturn(Optional.of(convRow));

            chatService.markAsRead(CONV_ID.toString(), "student@test.ru");

            verify(conversationRepo).markAsRead(2L, CONV_ID);
        }

        @Test
        @DisplayName("throws when not a participant")
        void notParticipant() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));
            when(conversationRepo.findConversationEntry(2L, CONV_ID)).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> chatService.markAsRead(CONV_ID.toString(), "student@test.ru"));
        }

        @Test
        @DisplayName("throws on invalid conversation id")
        void invalidId() {
            when(conversationRepo.isAvailable()).thenReturn(true);
            when(usersRepository.findByEmail("student@test.ru")).thenReturn(Optional.of(student));

            assertThrows(BadRequestException.class,
                    () -> chatService.markAsRead("bad-id", "student@test.ru"));
        }
    }
}
