package org.example.service;

import org.example.dto.response.AuditLogResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditService {
    void log(Long userId, String action, String entityType, Long entityId, String details);

    List<AuditLogResponse> search(Long userId, String action, String entityType,
                                  LocalDateTime from, LocalDateTime to, Long restrictToUniversityId);
}
