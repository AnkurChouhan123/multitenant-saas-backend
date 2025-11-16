package com.saas.platform.service;

import com.saas.platform.model.ActivityLog;
import com.saas.platform.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityLogService {
    
    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);
    
    private final ActivityLogRepository activityLogRepository;
    
    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }
    
    /**
     * Log an activity
     */
    @Transactional
    public void logActivity(Long tenantId, Long userId, String userEmail, String userName,
                           String action, String actionType, String details) {
        try {
            String ipAddress = getClientIpAddress();
            
            ActivityLog log = new ActivityLog(
                tenantId, userId, userEmail, userName,
                action, actionType, ipAddress, details
            );
            
            activityLogRepository.save(log);
            this.log.debug("Activity logged: {} by {}", action, userEmail);
            
        } catch (Exception e) {
            this.log.error("Failed to log activity: {}", e.getMessage());
        }
    }
    
    /**
     * Get all activities for a tenant
     */
    public List<ActivityLog> getActivitiesByTenant(Long tenantId) {
        return activityLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
    
    /**
     * Get activities with pagination
     */
    public Page<ActivityLog> getActivitiesPage(Long tenantId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return activityLogRepository.findByTenantId(tenantId, pageRequest);
    }
    
    /**
     * Get activities by type
     */
    public List<ActivityLog> getActivitiesByType(Long tenantId, String actionType) {
        return activityLogRepository.findByTenantIdAndActionType(tenantId, actionType);
    }
    
    /**
     * Get activities in date range
     */
    public List<ActivityLog> getActivitiesByDateRange(Long tenantId, LocalDateTime start, LocalDateTime end) {
        return activityLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end);
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP: {}", e.getMessage());
        }
        return "unknown";
    }
}
