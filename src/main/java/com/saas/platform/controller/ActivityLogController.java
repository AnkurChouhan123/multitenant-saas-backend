
package com.saas.platform.controller;

import com.saas.platform.model.ActivityLog;
import com.saas.platform.service.ActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = "http://localhost:3000")
public class ActivityLogController {
    
    private final ActivityLogService activityLogService;
    
    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }
    
    //view tenant activities
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<ActivityLog>> getActivitiesByTenant(@PathVariable Long tenantId) {
        List<ActivityLog> activities = activityLogService.getActivitiesByTenant(tenantId);
        return ResponseEntity.ok(activities);
    }
    
    //anyone can view activity page
    @GetMapping("/tenant/{tenantId}/page")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<Page<ActivityLog>> getActivitiesPage(
            @PathVariable Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ActivityLog> activities = activityLogService.getActivitiesPage(tenantId, page, size);
        return ResponseEntity.ok(activities);
    }
    
    // filter activities by type
    @GetMapping("/tenant/{tenantId}/type/{actionType}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<ActivityLog>> getActivitiesByType(
            @PathVariable Long tenantId,
            @PathVariable String actionType) {
        List<ActivityLog> activities = activityLogService.getActivitiesByType(tenantId, actionType);
        return ResponseEntity.ok(activities);
    }
    
    // can see activities by date ranges
    @GetMapping("/tenant/{tenantId}/range")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<List<ActivityLog>> getActivitiesByDateRange(
            @PathVariable Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<ActivityLog> activities = activityLogService.getActivitiesByDateRange(tenantId, start, end);
        return ResponseEntity.ok(activities);
    }
}
