package org.example.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class InAppNotificationResponse {
    Long id;
    String kind;
    String title;
    String body;
    Instant readAt;
    Instant createdAt;
}
