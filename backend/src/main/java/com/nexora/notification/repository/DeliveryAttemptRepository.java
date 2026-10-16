package com.nexora.notification.repository;

import com.nexora.notification.entity.DeliveryAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {

    @Query("""
            select a from DeliveryAttempt a
            where :userId is null or a.notificationId in (select n.id from Notification n where n.recipientId = :userId)
            """)
    Page<DeliveryAttempt> search(@Param("userId") Long userId, Pageable pageable);

    List<DeliveryAttempt> findByNotificationIdOrderByIdAsc(Long notificationId);

    @Query("select coalesce(max(a.attemptNo), 0) from DeliveryAttempt a where a.notificationId = :nid and a.channel = :channel")
    int lastAttemptNo(@Param("nid") Long notificationId,
                      @Param("channel") com.nexora.notification.entity.NotificationChannel channel);
}
