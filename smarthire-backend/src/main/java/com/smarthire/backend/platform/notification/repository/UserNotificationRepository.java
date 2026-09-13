package com.smarthire.backend.platform.notification.repository;

import com.smarthire.backend.platform.notification.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification,Long> {
    List<UserNotification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserNotification> findTop50ByUserIdAndDueAtIsNotNullOrderByDueAtAsc(Long userId);
    @Query("select n from UserNotification n where n.userId = :userId and (n.dueAt is null or n.dueAt <= :dueAt) order by n.createdAt desc")
    List<UserNotification> findTop50VisibleByUserId(@Param("userId") Long userId, @Param("dueAt") java.time.LocalDateTime dueAt);
    List<UserNotification> findByDueAtLessThanEqualAndDeliveredAtIsNull(java.time.LocalDateTime now);
    boolean existsByEventKey(String eventKey);
}
