package com.saas.platform.controller;

import com.saas.platform.dto.AnalyticsDashboardDto;
import com.saas.platform.model.ActivityLog;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AnalyticsController with proper permissions:
 * - TENANT_OWNER, TENANT_ADMIN: Full access ✅
 * - USER: No access ❌
 * - VIEWER: No access ❌
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final RoleValidator roleValidator;
    
    public AnalyticsController(AnalyticsService analyticsService,
                              RoleValidator roleValidator) {
        this.analyticsService = analyticsService;
        this.roleValidator = roleValidator;
    }
    
    /**
     * View analytics dashboard - ADMIN ONLY
     */
    @GetMapping("/dashboard/{tenantId}")
    public ResponseEntity<?> getDashboard(@PathVariable Long tenantId) {
        try {
            // Only TENANT_OWNER and TENANT_ADMIN can view analytics
            roleValidator.requireDetailedLogPermission(tenantId);
            
            AnalyticsDashboardDto dashboard = analyticsService.getDashboardMetrics(tenantId);
            return ResponseEntity.ok(dashboard);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * View activities by range - ADMIN ONLY
     */
    @GetMapping("/activities/range")
    public ResponseEntity<?> getActivitiesByRange(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            // Only TENANT_OWNER and TENANT_ADMIN can view analytics
            roleValidator.requireDetailedLogPermission(tenantId);
            
            List<ActivityLog> activities = analyticsService.getActivitiesByDateRange(
                tenantId, startDate, endDate);
            return ResponseEntity.ok(activities);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * View activities by type - ADMIN ONLY
     */
    @GetMapping("/activities/type")
    public ResponseEntity<?> getActivitiesByType(
            @RequestParam Long tenantId,
            @RequestParam String actionType) {
        try {
            // Only TENANT_OWNER and TENANT_ADMIN can view analytics
            roleValidator.requireDetailedLogPermission(tenantId);
            
            List<ActivityLog> activities = analyticsService.getActivitiesByType(tenantId, actionType);
            return ResponseEntity.ok(activities);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check if current user can access analytics
     */
    @GetMapping("/check-permission/{tenantId}")
    public ResponseEntity<Map<String, Object>> checkPermission(@PathVariable Long tenantId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("canViewAnalytics", roleValidator.canViewDetailedLogs(tenantId));
            response.put("role", roleValidator.getCurrentUser().getRole().toString());
        } catch (Exception e) {
            response.put("canViewAnalytics", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    // Helper method
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}