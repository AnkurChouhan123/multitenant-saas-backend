package com.saas.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final ActivityLogService activityLogService;
    
    public AuditService(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }
    
 
    @Transactional
    public void auditChange(Long tenantId, String userId, String entityType, 
                           String action, Object before, Object after) {
        String details = String.format("Changed %s from %s to %s", 
            entityType, before, after);
        
        activityLogService.logActivity(
            tenantId, 
            Long.parseLong(userId), 
            "", 
            "",
            "Updated " + entityType, 
            "data", 
            details
        );
    }
}