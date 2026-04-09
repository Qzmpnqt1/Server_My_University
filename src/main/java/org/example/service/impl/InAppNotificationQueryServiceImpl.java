package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dto.response.InAppNotificationResponse;
import org.example.exception.AccessDeniedException;
import org.example.exception.ResourceNotFoundException;
import org.example.model.InAppNotification;
import org.example.model.Users;
import org.example.repository.InAppNotificationRepository;
import org.example.repository.UsersRepository;
import org.example.service.InAppNotificationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InAppNotificationQueryServiceImpl implements InAppNotificationQueryService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional(readOnly = true)
    public List<InAppNotificationResponse> listMine(String email) {
        Long uid = requireUserId(email);
        return inAppNotificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(uid).stream()
                .map(this::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String email) {
        Long uid = requireUserId(email);
        return inAppNotificationRepository.countByUserIdAndReadAtIsNull(uid);
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, String email) {
        Long uid = requireUserId(email);
        InAppNotification n = inAppNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Уведомление не найдено"));
        if (!n.getUser().getId().equals(uid)) {
            throw new AccessDeniedException("Нет доступа к этому уведомлению");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            inAppNotificationRepository.save(n);
        }
    }

    @Override
    @Transactional
    public void markAllRead(String email) {
        Long uid = requireUserId(email);
        List<InAppNotification> list = inAppNotificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(uid);
        Instant now = Instant.now();
        for (InAppNotification n : list) {
            if (n.getReadAt() == null) {
                n.setReadAt(now);
                inAppNotificationRepository.save(n);
            }
        }
    }

    private Long requireUserId(String email) {
        return usersRepository.findByEmail(email)
                .map(Users::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private InAppNotificationResponse map(InAppNotification n) {
        return InAppNotificationResponse.builder()
                .id(n.getId())
                .kind(n.getKind())
                .title(n.getTitle())
                .body(n.getBody())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
