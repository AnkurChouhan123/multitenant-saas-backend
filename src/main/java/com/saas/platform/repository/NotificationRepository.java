package com.saas.platform.repository;

import com.saas.platform.model.Notification;
import com.saas.platform.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

//
// NotificationRepository - Database operations for Notifications
 
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find all notifications for a user
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find unread notifications
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    
    // Find read notifications
    List<Notification> findByUserIdAndIsReadTrue(Long userId);
    
    // Count unread notifications
    long countByUserIdAndIsReadFalse(Long userId);
    
    // Find notifications with pagination
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    // Find by type
    List<Notification> findByUserIdAndType(Long userId, NotificationType type);
    
    // Find recent notifications
    List<Notification> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
        Long userId, LocalDateTime after);
    
    // Find old read notifications (for cleanup)
    List<Notification> findByCreatedAtBeforeAndIsReadTrue(LocalDateTime before);
    
    // Find notifications by tenant
    List<Notification> findByTenantId(Long tenantId);
    
    // Find by priority
    List<Notification> findByUserIdAndPriorityOrderByCreatedAtDesc(
        Long userId, String priority);
    
    // Find expired notifications
    List<Notification> findByExpiresAtBefore(LocalDateTime dateTime);
}