package org.example.service;

import org.example.dto.response.InAppNotificationResponse;

import java.util.List;

public interface InAppNotificationQueryService {

    List<InAppNotificationResponse> listMine(String email);

    long countUnread(String email);

    void markRead(Long notificationId, String email);

    void markAllRead(String email);
}
