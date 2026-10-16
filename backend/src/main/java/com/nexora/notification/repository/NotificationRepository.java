package com.nexora.notification.repository;

import com.nexora.notification.entity.Notification;
import com.nexora.notification.entity.NotificationCategory;
import com.nexora.notification.entity.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            select n from Notification n
            where n.recipientId = :userId
              and (:unreadOnly = false or n.readAt is null)
              and (:severity is null or n.severity = :severity)
              and (:category is null or n.category = :category)
            """)
    Page<Notification> search(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly,
                              @Param("severity") Severity severity, @Param("category") NotificationCategory category,
                              Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipientId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    List<Notification> findTop5ByRecipientIdOrderByCreatedAtDesc(Long userId);

    @Query("select n from Notification n where n.createdAt >= :since")
    List<Notification> findSince(@Param("since") Instant since);
}
