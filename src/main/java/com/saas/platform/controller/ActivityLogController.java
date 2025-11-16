package com.saas.platform.controller;

import com.saas.platform.model.ActivityLog;
import com.saas.platform.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ActivityLogController - REST API for activity logs
 */
@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "http://localhost:3000")
public class ActivityLogController {
    
    private final ActivityLogService activityLogService;
    
    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }
    
    /**
     * GET /api/activities/tenant/{tenantId} - Get all activities for tenant
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<ActivityLog>> getActivitiesByTenant(@PathVariable Long tenantId) {
        List<ActivityLog> activities = activityLogService.getActivitiesByTenant(tenantId);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * GET /api/activities/tenant/{tenantId}/page - Get activities with pagination
     */
    @GetMapping("/tenant/{tenantId}/page")
    public ResponseEntity<Page<ActivityLog>> getActivitiesPage(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLog> activities = activityLogService.getActivitiesPage(tenantId, page, size);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * GET /api/activities/tenant/{tenantId}/type/{actionType} - Filter by type
     */
    @GetMapping("/tenant/{tenantId}/type/{actionType}")
    public ResponseEntity<List<ActivityLog>> getActivitiesByType(
            @PathVariable Long tenantId,
            @PathVariable String actionType) {
        List<ActivityLog> activities = activityLogService.getActivitiesByType(tenantId, actionType);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * GET /api/activities/tenant/{tenantId}/range - Get activities in date range
     */
    @GetMapping("/tenant/{tenantId}/range")
    public ResponseEntity<List<ActivityLog>> getActivitiesByDateRange(
            @PathVariable Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ActivityLog> activities = activityLogService.getActivitiesByDateRange(tenantId, start, end);
        return ResponseEntity.ok(activities);
    }
}