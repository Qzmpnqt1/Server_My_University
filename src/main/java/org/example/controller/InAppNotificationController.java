package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.response.InAppNotificationResponse;
import org.example.service.InAppNotificationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "In-app notifications", description = "Персистентные уведомления для мобильного клиента")
public class InAppNotificationController {

    private final InAppNotificationQueryService inAppNotificationQueryService;

    @GetMapping("/my")
    @Operation(summary = "Список уведомлений текущего пользователя (до 50 последних)")
    public ResponseEntity<List<InAppNotificationResponse>> myNotifications(Principal principal) {
        return ResponseEntity.ok(inAppNotificationQueryService.listMine(principal.getName()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Число непрочитанных уведомлений")
    public ResponseEntity<Map<String, Long>> unreadCount(Principal principal) {
        long c = inAppNotificationQueryService.countUnread(principal.getName());
        return ResponseEntity.ok(Map.of("unreadCount", c));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Отметить уведомление прочитанным")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Principal principal) {
        inAppNotificationQueryService.markRead(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Отметить все текущие уведомления прочитанными")
    public ResponseEntity<Void> markAllRead(Principal principal) {
        inAppNotificationQueryService.markAllRead(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
