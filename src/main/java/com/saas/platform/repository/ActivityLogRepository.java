package com.saas.platform.repository;

import com.saas.platform.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    List<ActivityLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    
    Page<ActivityLog> findByTenantId(Long tenantId, Pageable pageable);
    
    List<ActivityLog> findByTenantIdAndActionType(Long tenantId, String actionType);
    
    List<ActivityLog> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime start, LocalDateTime end);
}