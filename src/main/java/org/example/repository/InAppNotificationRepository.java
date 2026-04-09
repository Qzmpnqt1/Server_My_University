package org.example.repository;

import org.example.model.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);
}
