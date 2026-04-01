package org.example.service.impl;

import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.example.dto.request.SendMessageRequest;
import org.example.dto.response.ChatContactResponse;
import org.example.dto.response.ConversationResponse;
import org.example.dto.response.MessageResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.BadRequestException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.*;
import org.example.repository.StudentProfileRepository;
import org.example.repository.TeacherProfileRepository;
import org.example.repository.UsersRepository;
import org.example.repository.cassandra.CassandraConversationRepository;
import org.example.repository.cassandra.CassandraMessageRepository;
import org.example.service.ChatService;
import org.example.service.NotificationService;
import org.example.service.UniversityScopeService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    private final UsersRepository usersRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CassandraConversationRepository conversationRepo;
    private final CassandraMessageRepository messageRepo;
    private final NotificationService notificationService;
    private final UniversityScopeService universityScopeService;

    public ChatServiceImpl(UsersRepository usersRepository,
                           StudentProfileRepository studentProfileRepository,
                           TeacherProfileRepository teacherProfileRepository,
                           CassandraConversationRepository conversationRepo,
                           CassandraMessageRepository messageRepo,
                           NotificationService notificationService,
                           UniversityScopeService universityScopeService) {
        this.usersRepository = usersRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.notificationService = notificationService;
        this.universityScopeService = universityScopeService;
    }

    private void ensureCassandraAvailable() {
        if (!conversationRepo.isAvailable()) {
            throw new BadRequestException("Чат-сервис недоступен");
        }
    }

    @Override
    public List<ChatContactResponse> listChatContacts(String email) {
        Users self = findUserByEmail(email);
        Long scopeUni = requireUniversityIdForMessaging(email, self);
        return usersRepository.findAll().stream()
                .filter(u -> !u.getId().equals(self.getId()))
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> universityScopeService.userBelongsToUniversity(u.getId(), scopeUni))
                .sorted(Comparator.comparing(Users::getLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Users::getFirstName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(u -> ChatContactResponse.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .middleName(u.getMiddleName())
                        .userType(u.getUserType() != null ? u.getUserType().name() : null)
                        .build())
                .toList();
    }

    @Override
    public List<ConversationResponse> getMyConversations(String email) {
        ensureCassandraAvailable();
        Users user = findUserByEmail(email);

        return conversationRepo.findByUserId(user.getId()).stream()
                .map(this::mapConversation)
                .toList();
    }

    @Override
    public List<MessageResponse> getMessages(String conversationId, String email,
                                             int limit, Instant before) {
        ensureCassandraAvailable();
        Users user = findUserByEmail(email);
        UUID convUuid = parseConversationId(conversationId);

        verifyParticipant(user.getId(), convUuid);

        int effectiveLimit = Math.min(Math.max(limit, 1), 200);
        List<Row> rows = (before != null)
                ? messageRepo.findByConversationIdBefore(convUuid, before, effectiveLimit)
                : messageRepo.findByConversationId(convUuid, effectiveLimit);

        return rows.stream().map(this::mapMessage).toList();
    }

    @Override
    public MessageResponse sendMessage(SendMessageRequest request, String email) {
        ensureCassandraAvailable();
        Users sender = findUserByEmail(email);
        Users recipient = usersRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Получатель не найден"));

        if (sender.getId().equals(recipient.getId())) {
            throw new BadRequestException("Нельзя отправить сообщение самому себе");
        }

        Long senderUni = requireUniversityIdForMessaging(email, sender);
        if (!universityScopeService.userBelongsToUniversity(recipient.getId(), senderUni)) {
            throw new BadRequestException("Можно отправлять сообщения только пользователям своего вуза");
        }

        UUID conversationId = generateConversationId(sender.getId(), recipient.getId());
        UUID messageId = Uuids.timeBased();
        Instant now = Instant.now();
        String senderFullName = buildFullName(sender);
        String recipientFullName = buildFullName(recipient);

        messageRepo.insert(conversationId, messageId, sender.getId(),
                senderFullName, request.getText(), now);

        conversationRepo.deleteOldAndInsertNew(
                sender.getId(), conversationId, recipient.getId(),
                recipientFullName, request.getText(), now, 0);
        conversationRepo.deleteOldAndInsertNew(
                recipient.getId(), conversationId, sender.getId(),
                senderFullName, request.getText(), now, 1);

        notificationService.notifyNewChatMessage(recipient.getId(), senderFullName);

        return MessageResponse.builder()
                .messageId(messageId.toString())
                .conversationId(conversationId.toString())
                .senderId(sender.getId())
                .senderName(senderFullName)
                .text(request.getText())
                .sentAt(now)
                .build();
    }

    @Override
    public void markAsRead(String conversationId, String email) {
        ensureCassandraAvailable();
        Users user = findUserByEmail(email);
        UUID convUuid = parseConversationId(conversationId);

        verifyParticipant(user.getId(), convUuid);
        conversationRepo.markAsRead(user.getId(), convUuid);
    }

    private Users findUserByEmail(String email) {
        return usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    /**
     * Вуз текущего пользователя для списка контактов и отправки сообщений (студент / преподаватель / админ).
     */
    private Long requireUniversityIdForMessaging(String email, Users self) {
        return switch (self.getUserType()) {
            case ADMIN -> universityScopeService.requireAdminUniversityId(email);
            case STUDENT -> studentProfileRepository.findByUserId(self.getId())
                    .map(sp -> sp.getInstitute().getUniversity().getId())
                    .orElseThrow(() -> new BadRequestException("Профиль студента не найден или не привязан к вузу"));
            case TEACHER -> teacherProfileRepository.findByUserId(self.getId())
                    .map(tp -> tp.getInstitute() != null ? tp.getInstitute().getUniversity().getId() : null)
                    .filter(Objects::nonNull)
                    .orElseThrow(() -> new BadRequestException("Профиль преподавателя не привязан к институту вуза"));
            default -> throw new AccessDeniedException("Чат недоступен для данного типа пользователя");
        };
    }

    private UUID parseConversationId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный формат ID беседы");
        }
    }

    private void verifyParticipant(Long userId, UUID conversationId) {
        if (conversationRepo.findConversationEntry(userId, conversationId).isEmpty()) {
            throw new BadRequestException("У вас нет доступа к этой беседе");
        }
    }

    private ConversationResponse mapConversation(Row row) {
        return ConversationResponse.builder()
                .conversationId(row.getUuid("conversation_id").toString())
                .participantId(row.getLong("participant_id"))
                .participantName(row.getString("participant_name"))
                .lastMessageText(row.getString("last_message_text"))
                .lastMessageAt(row.getInstant("last_message_at"))
                .unreadCount(row.getInt("unread_count"))
                .build();
    }

    private MessageResponse mapMessage(Row row) {
        return MessageResponse.builder()
                .messageId(row.getUuid("message_id").toString())
                .senderId(row.getLong("sender_id"))
                .senderName(row.getString("sender_name"))
                .text(row.getString("text"))
                .sentAt(row.getInstant("sent_at"))
                .build();
    }

    private String buildFullName(Users user) {
        return user.getLastName() + " " + user.getFirstName();
    }

    public static UUID generateConversationId(Long userId1, Long userId2) {
        long smaller = Math.min(userId1, userId2);
        long larger = Math.max(userId1, userId2);
        String seed = smaller + ":" + larger;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
