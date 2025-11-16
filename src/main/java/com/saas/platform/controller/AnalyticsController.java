
package com.saas.platform.controller;

import com.saas.platform.dto.AnalyticsDashboardDto;
import com.saas.platform.model.ActivityLog;
import com.saas.platform.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    /**
     * GET /api/analytics/dashboard/{tenantId} - Get complete dashboard metrics
     */
    @GetMapping("/dashboard/{tenantId}")
    public ResponseEntity<AnalyticsDashboardDto> getDashboard(@PathVariable Long tenantId) {
        AnalyticsDashboardDto dashboard = analyticsService.getDashboardMetrics(tenantId);
        return ResponseEntity.ok(dashboard);
    }
    
    /**
     * GET /api/analytics/activities/range - Get activities in date range
     */
    @GetMapping("/activities/range")
    public ResponseEntity<List<ActivityLog>> getActivitiesByRange(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<ActivityLog> activities = analyticsService.getActivitiesByDateRange(
            tenantId, startDate, endDate);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * GET /api/analytics/activities/type - Get activities by type
     */
    @GetMapping("/activities/type")
    public ResponseEntity<List<ActivityLog>> getActivitiesByType(
            @RequestParam Long tenantId,
            @RequestParam String actionType) {
        
        List<ActivityLog> activities = analyticsService.getActivitiesByType(tenantId, actionType);
        return ResponseEntity.ok(activities);
    }
}