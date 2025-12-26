package com.saas.platform.controller;

import com.saas.platform.dto.PlatformStatsDto;
import com.saas.platform.dto.TenantManagementDto;
import com.saas.platform.model.Tenant;
import com.saas.platform.model.TenantStatus;
import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.service.SuperAdminService;
import com.saas.platform.security.RoleValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//
// SuperAdminController - Platform Management Only
// 
// SUPER ADMIN manages the PLATFORM, not tenant internals.
// - Tenant management (create, suspend, activate, delete)
// - Subscription plans (create, modify, assign)
// - Global analytics (aggregates only)
// - Platform configuration
// - Security & compliance
// - Integrations
// 
// ❌ DOES NOT manage:
// - Tenant users
// - Tenant settings
// - Tenant data/files
// - Tenant webhooks
// - Tenant activity logs
 
@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {
    
    private final SuperAdminService superAdminService;
    private final RoleValidator roleValidator;
    
    public SuperAdminController(SuperAdminService superAdminService,
                               RoleValidator roleValidator) {
        this.superAdminService = superAdminService;
        this.roleValidator = roleValidator;
    }
    
    // ========================================
    // PLATFORM OVERVIEW & STATS
    // ========================================
    
    //
// Get platform-wide statistics (aggregates only)
     
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDto> getPlatformStats() {
        PlatformStatsDto stats = superAdminService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }
    
    //
// Get platform health status
     
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getPlatformHealth() {
        Map<String, Object> health = superAdminService.getPlatformHealth();
        return ResponseEntity.ok(health);
    }
    
    // ========================================
    // TENANT MANAGEMENT
    // ========================================
    
    //
// Get all tenants (metadata only - no internal data)
     
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantManagementDto>> getAllTenants() {
        List<TenantManagementDto> tenants = superAdminService.getAllTenantsForManagement();
        return ResponseEntity.ok(tenants);
    }
    
    //
// Get single tenant metadata
     
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantManagementDto> getTenantMetadata(@PathVariable Long tenantId) {
        TenantManagementDto tenant = superAdminService.getTenantMetadata(tenantId);
        return ResponseEntity.ok(tenant);
    }
    
    //
// Create new tenant
     
    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant created = superAdminService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    //
// Suspend tenant (stops access but preserves data)
     
    @PutMapping("/tenants/{tenantId}/suspend")
    public ResponseEntity<Map<String, String>> suspendTenant(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String reason) {
        superAdminService.suspendTenant(tenantId, reason);
        return ResponseEntity.ok(Map.of("message", "Tenant suspended successfully"));
    }
    
    //
// Activate tenant (resume access)
     
    @PutMapping("/tenants/{tenantId}/activate")
    public ResponseEntity<Map<String, String>> activateTenant(@PathVariable Long tenantId) {
        superAdminService.activateTenant(tenantId);
        return ResponseEntity.ok(Map.of("message", "Tenant activated successfully"));
    }
    
    //
// Force logout all users of a tenant
     
    @PostMapping("/tenants/{tenantId}/force-logout")
    public ResponseEntity<Map<String, String>> forceLogoutTenant(@PathVariable Long tenantId) {
        superAdminService.forceLogoutAllUsers(tenantId);
        return ResponseEntity.ok(Map.of("message", "All users logged out"));
    }
    
    //
// Soft delete tenant
     
    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Map<String, String>> deleteTenant(@PathVariable Long tenantId) {
        superAdminService.softDeleteTenant(tenantId);
        return ResponseEntity.ok(Map.of("message", "Tenant deleted successfully"));
    }
    
    //
// Impersonate tenant owner (generate temporary token)
     
    @PostMapping("/tenants/{tenantId}/impersonate")
    public ResponseEntity<Map<String, String>> impersonateTenantOwner(@PathVariable Long tenantId) {
        String token = superAdminService.impersonateTenantOwner(tenantId);
        return ResponseEntity.ok(Map.of("token", token, "message", "Impersonation token generated"));
    }
    
    // ========================================
    // SUBSCRIPTION PLAN MANAGEMENT
    // ========================================
    
    //
// Get all subscription plans
     
    @GetMapping("/plans")
    public ResponseEntity<List<Map<String, Object>>> getAllPlans() {
        List<Map<String, Object>> plans = superAdminService.getAllPlansWithStats();
        return ResponseEntity.ok(plans);
    }
    
    //
// Create new subscription plan
     
    @PostMapping("/plans")
    public ResponseEntity<Map<String, String>> createPlan(@RequestBody Map<String, Object> planData) {
        superAdminService.createSubscriptionPlan(planData);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Plan created successfully"));
    }
    
    //
// Update plan pricing/limits
     
    @PutMapping("/plans/{planName}")
    public ResponseEntity<Map<String, String>> updatePlan(
            @PathVariable String planName,
            @RequestBody Map<String, Object> planData) {
        superAdminService.updatePlanPricing(planName, planData);
        return ResponseEntity.ok(Map.of("message", "Plan updated successfully"));
    }
    
    //
// Assign plan to tenant
     
    @PostMapping("/tenants/{tenantId}/assign-plan")
    public ResponseEntity<Map<String, String>> assignPlan(
            @PathVariable Long tenantId,
            @RequestParam String planName) {
        superAdminService.assignPlanToTenant(tenantId, planName);
        return ResponseEntity.ok(Map.of("message", "Plan assigned successfully"));
    }
    
    //
// Get all subscriptions across platform
     
    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, Object>>> getAllSubscriptions() {
        List<Map<String, Object>> subscriptions = superAdminService.getAllSubscriptionsWithRevenue();
        return ResponseEntity.ok(subscriptions);
    }
    
    // ========================================
    // GLOBAL ANALYTICS (AGGREGATES ONLY)
    // ========================================
    
    //
// Get global analytics (no tenant-specific data)
     
    @GetMapping("/analytics/global")
    public ResponseEntity<Map<String, Object>> getGlobalAnalytics() {
        Map<String, Object> analytics = superAdminService.getGlobalAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    //
// Get platform usage statistics
     
    @GetMapping("/analytics/usage")
    public ResponseEntity<Map<String, Object>> getPlatformUsage() {
        Map<String, Object> usage = Map.of(
            "totalStorage", superAdminService.getTotalStorageUsed(),
            "totalApiCalls", superAdminService.getTotalApiCalls(),
            "totalWebhooks", superAdminService.getTotalWebhooks(),
            "errorRate", superAdminService.getGlobalErrorRate()
        );
        return ResponseEntity.ok(usage);
    }
    
    //
// Get revenue analytics
     
    @GetMapping("/analytics/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueAnalytics() {
        Map<String, Object> revenue = superAdminService.getRevenueAnalytics();
        return ResponseEntity.ok(revenue);
    }
    
    // ========================================
    // SECURITY & COMPLIANCE
    // ========================================
    
    //
// Get global audit logs (platform-level only)
     
    @GetMapping("/security/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getGlobalAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> logs = superAdminService.getGlobalAuditLogs(page, size);
        return ResponseEntity.ok(logs);
    }
    
    //
// Get security alerts
     
    @GetMapping("/security/alerts")
    public ResponseEntity<List<Map<String, Object>>> getSecurityAlerts() {
        List<Map<String, Object>> alerts = superAdminService.getSecurityAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    //
// Get login history across platform
     
    @GetMapping("/security/login-history")
    public ResponseEntity<Map<String, Object>> getGlobalLoginHistory() {
        Map<String, Object> history = superAdminService.getGlobalLoginHistory();
        return ResponseEntity.ok(history);
    }
    
    //
// Force password reset for any user
     
    @PostMapping("/security/force-password-reset")
    public ResponseEntity<Map<String, String>> forcePasswordReset(@RequestParam String email) {
        superAdminService.forcePasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }
    
    //
// Disable compromised account
     
    @PostMapping("/security/disable-account")
    public ResponseEntity<Map<String, String>> disableCompromisedAccount(@RequestParam Long userId) {
        superAdminService.disableAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account disabled"));
    }
    
    // ========================================
    // PLATFORM CONFIGURATION
    // ========================================
    
    //
// Get platform configuration
     
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPlatformConfig() {
        Map<String, Object> config = superAdminService.getPlatformConfiguration();
        return ResponseEntity.ok(config);
    }
    
    //
// Update platform configuration
     
    @PutMapping("/config")
    public ResponseEntity<Map<String, String>> updatePlatformConfig(
            @RequestBody Map<String, Object> config) {
        superAdminService.updatePlatformConfiguration(config);
        return ResponseEntity.ok(Map.of("message", "Configuration updated"));
    }
    
    //
// Toggle maintenance mode
     
    @PostMapping("/config/maintenance-mode")
    public ResponseEntity<Map<String, String>> toggleMaintenanceMode(@RequestParam boolean enabled) {
        superAdminService.setMaintenanceMode(enabled);
        return ResponseEntity.ok(Map.of(
            "message", enabled ? "Maintenance mode enabled" : "Maintenance mode disabled"
        ));
    }
    
    //
// Update feature flags
     
    @PutMapping("/config/feature-flags")
    public ResponseEntity<Map<String, String>> updateFeatureFlags(
            @RequestBody Map<String, Boolean> flags) {
        superAdminService.updateFeatureFlags(flags);
        return ResponseEntity.ok(Map.of("message", "Feature flags updated"));
    }
    
    
    
    //
// Get platform integrations status
     
    @GetMapping("/integrations")
    public ResponseEntity<Map<String, Object>> getIntegrations() {
        Map<String, Object> integrations = superAdminService.getIntegrationStatus();
        return ResponseEntity.ok(integrations);
    }
    
    //
// Update payment gateway configuration
     
    @PutMapping("/integrations/payment")
    public ResponseEntity<Map<String, String>> updatePaymentGateway(
            @RequestBody Map<String, String> config) {
        superAdminService.updatePaymentGatewayConfig(config);
        return ResponseEntity.ok(Map.of("message", "Payment gateway updated"));
    }
    
    //
// Update email provider configuration
     
    @PutMapping("/integrations/email")
    public ResponseEntity<Map<String, String>> updateEmailProvider(
            @RequestBody Map<String, String> config) {
        superAdminService.updateEmailProviderConfig(config);
        return ResponseEntity.ok(Map.of("message", "Email provider updated"));
    }
    
    
    //
// Get system errors
     
    @GetMapping("/monitoring/errors")
    public ResponseEntity<List<Map<String, Object>>> getSystemErrors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> errors = superAdminService.getSystemErrors(page, size);
        return ResponseEntity.ok(errors);
    }
    
    //
// Get backend logs
     
    @GetMapping("/monitoring/logs")
    public ResponseEntity<List<String>> getBackendLogs(
            @RequestParam(defaultValue = "100") int lines) {
        List<String> logs = superAdminService.getRecentLogs(lines);
        return ResponseEntity.ok(logs);
    }
    
    //
// Retry failed jobs
     
    @PostMapping("/monitoring/retry-jobs")
    public ResponseEntity<Map<String, String>> retryFailedJobs() {
        int retried = superAdminService.retryFailedJobs();
        return ResponseEntity.ok(Map.of("message", retried + " jobs retried"));
    }
    
 // ========================================
 // ADD THESE METHODS TO YOUR EXISTING SuperAdminController.java
 // Add them BEFORE the closing brace of the class (around line 270)
 // ========================================

 // ========================================
 // TENANT DATA EXPORT & BACKUP
 // ========================================

 //
// Export tenant data for backup or migration
  
 @PostMapping("/tenants/{tenantId}/export")
 public ResponseEntity<Resource> exportTenantData(@PathVariable Long tenantId) {
     Resource dataExport = superAdminService.exportTenantData(tenantId);
     return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tenant_" + tenantId + "_export.zip")
         .contentType(MediaType.APPLICATION_OCTET_STREAM)
         .body(dataExport);
 }

 //
// Bulk export all tenants data
  
 @PostMapping("/tenants/bulk-export")
 public ResponseEntity<Resource> bulkExportAllTenants() {
     Resource dataExport = superAdminService.bulkExportAllTenants();
     return ResponseEntity.ok()
         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all_tenants_export.zip")
         .body(dataExport);
 }

 // ========================================
 // ADVANCED TENANT SEARCH & FILTERING
 // ========================================

 //
// Advanced tenant search with filters
  
 @GetMapping("/tenants/search")
 public ResponseEntity<List<TenantManagementDto>> searchTenants(
         @RequestParam(required = false) String name,
         @RequestParam(required = false) String status,
         @RequestParam(required = false) String plan,
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
         @RequestParam(required = false) Boolean subscriptionActive,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size) {
     
     List<TenantManagementDto> results = superAdminService.searchTenants(
         name, status, plan, createdAfter, createdBefore, subscriptionActive, page, size);
     return ResponseEntity.ok(results);
 }

 // ========================================
 // BULK OPERATIONS
 // ========================================

 //
// Bulk suspend multiple tenants
  
 @PostMapping("/tenants/bulk-suspend")
 public ResponseEntity<Map<String, Object>> bulkSuspendTenants(
         @RequestBody List<Long> tenantIds,
         @RequestParam String reason) {
     Map<String, Object> result = superAdminService.bulkSuspendTenants(tenantIds, reason);
     return ResponseEntity.ok(result);
 }

 //
// Bulk activate multiple tenants
  
 @PostMapping("/tenants/bulk-activate")
 public ResponseEntity<Map<String, Object>> bulkActivateTenants(
         @RequestBody List<Long> tenantIds) {
     Map<String, Object> result = superAdminService.bulkActivateTenants(tenantIds);
     return ResponseEntity.ok(result);
 }

 //
// Bulk change subscription plans
  
 @PostMapping("/tenants/bulk-change-plan")
 public ResponseEntity<Map<String, Object>> bulkChangePlan(
         @RequestBody List<Long> tenantIds,
         @RequestParam String planName) {
     Map<String, Object> result = superAdminService.bulkChangePlan(tenantIds, planName);
     return ResponseEntity.ok(result);
 }

 // ========================================
 // REAL-TIME MONITORING DASHBOARD
 // ========================================

 //
// Get real-time platform metrics
  
 @GetMapping("/monitoring/realtime")
 public ResponseEntity<Map<String, Object>> getRealtimeMetrics() {
     Map<String, Object> metrics = superAdminService.getRealtimeMetrics();
     return ResponseEntity.ok(metrics);
 }

 //
// Get system resource alerts
  
 @GetMapping("/monitoring/resource-alerts")
 public ResponseEntity<List<Map<String, Object>>> getResourceAlerts() {
     List<Map<String, Object>> alerts = superAdminService.getResourceAlerts();
     return ResponseEntity.ok(alerts);
 }

 // ========================================
 // TENANT COMMUNICATION TOOLS
 // ========================================

 //
// Send announcement to specific tenants
  
 @PostMapping("/communications/send-announcement")
 public ResponseEntity<Map<String, String>> sendAnnouncement(
         @RequestBody Map<String, Object> announcement) {
     superAdminService.sendAnnouncementToTenants(
         (List<Long>) announcement.get("tenantIds"),
         (String) announcement.get("subject"),
         (String) announcement.get("message"),
         (String) announcement.get("priority")
     );
     return ResponseEntity.ok(Map.of("message", "Announcement sent successfully"));
 }

 //
// Send platform-wide notification
  
 @PostMapping("/communications/broadcast")
 public ResponseEntity<Map<String, String>> broadcastMessage(
         @RequestParam String subject,
         @RequestParam String message,
         @RequestParam(required = false) String urgency) {
     int recipientCount = superAdminService.broadcastToAllTenants(subject, message, urgency);
     return ResponseEntity.ok(Map.of(
         "message", "Broadcast sent",
         "recipients", String.valueOf(recipientCount)
     ));
 }

 // ========================================
 // USAGE ANALYTICS & REPORTING
 // ========================================

 //
// Generate custom platform report
  
 @PostMapping("/reports/generate")
 public ResponseEntity<Map<String, Object>> generateCustomReport(
         @RequestParam String reportType,
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
         @RequestParam(defaultValue = "JSON") String format) {
     
     Map<String, Object> report = superAdminService.generateReport(reportType, startDate, endDate, format);
     return ResponseEntity.ok(report);
 }

 //
// Get tenant usage trends
  
 @GetMapping("/analytics/usage-trends")
 public ResponseEntity<Map<String, Object>> getUsageTrends(
         @RequestParam(defaultValue = "30") int days) {
     Map<String, Object> trends = superAdminService.getUsageTrends(days);
     return ResponseEntity.ok(trends);
 }

 //
// Get churn analysis
  
 @GetMapping("/analytics/churn")
 public ResponseEntity<Map<String, Object>> getChurnAnalysis() {
     Map<String, Object> churnData = superAdminService.getChurnAnalysis();
     return ResponseEntity.ok(churnData);
 }

 // ========================================
 // AUTOMATED ALERTS & THRESHOLDS
 // ========================================

 //
// Configure platform alerts
  
 @PostMapping("/alerts/configure")
 public ResponseEntity<Map<String, String>> configureAlert(@RequestBody Map<String, Object> alertConfig) {
     superAdminService.configureAlert(
         (String) alertConfig.get("alertType"),
         (Number) alertConfig.get("threshold"),
         (String) alertConfig.get("notificationChannel")
     );
     return ResponseEntity.ok(Map.of("message", "Alert configured successfully"));
 }

 //
// Get triggered alerts history
  
 @GetMapping("/alerts/history")
 public ResponseEntity<List<Map<String, Object>>> getAlertHistory(
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "50") int size) {
     List<Map<String, Object>> alerts = superAdminService.getAlertHistory(page, size);
     return ResponseEntity.ok(alerts);
 }

 // ========================================
 // DATABASE MANAGEMENT TOOLS
 // ========================================

 //
// Get database statistics
  
 @GetMapping("/database/stats")
 public ResponseEntity<Map<String, Object>> getDatabaseStats() {
     Map<String, Object> stats = superAdminService.getDatabaseStats();
     return ResponseEntity.ok(stats);
 }

 //
// Run database optimization
  
 @PostMapping("/database/optimize")
 public ResponseEntity<Map<String, String>> optimizeDatabase() {
     Map<String, String> result = superAdminService.optimizeDatabase();
     return ResponseEntity.ok(result);
 }

 //
// Create database backup
  
 @PostMapping("/database/backup")
 public ResponseEntity<Map<String, String>> createDatabaseBackup() {
     String backupId = superAdminService.createDatabaseBackup();
     return ResponseEntity.ok(Map.of(
         "message", "Backup created successfully",
         "backupId", backupId
     ));
 }

 // ========================================
 // TENANT LIFECYCLE AUTOMATION
 // ========================================

 //
// Auto-cleanup inactive tenants
  
 @PostMapping("/automation/cleanup-inactive")
 public ResponseEntity<Map<String, Object>> cleanupInactiveTenants(
         @RequestParam int inactiveDays,
         @RequestParam boolean dryRun) {
     Map<String, Object> result = superAdminService.cleanupInactiveTenants(inactiveDays, dryRun);
     return ResponseEntity.ok(result);
 }

 //
// Auto-downgrade expired trials
  
 @PostMapping("/automation/handle-expired-trials")
 public ResponseEntity<Map<String, Object>> handleExpiredTrials() {
     Map<String, Object> result = superAdminService.handleExpiredTrials();
     return ResponseEntity.ok(result);
 }

 //
// Schedule tenant migration
  
 @PostMapping("/tenants/{tenantId}/schedule-migration")
 public ResponseEntity<Map<String, String>> scheduleTenantMigration(
         @PathVariable Long tenantId,
         @RequestParam String targetRegion,
         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime) {
     superAdminService.scheduleTenantMigration(tenantId, targetRegion, scheduledTime);
     return ResponseEntity.ok(Map.of("message", "Migration scheduled successfully"));
 }

 // ========================================
 // API RATE LIMITING & QUOTA MANAGEMENT
 // ========================================

 //
// Get tenant API usage
  
 @GetMapping("/tenants/{tenantId}/api-usage")
 public ResponseEntity<Map<String, Object>> getTenantApiUsage(
         @PathVariable Long tenantId,
         @RequestParam(defaultValue = "30") int days) {
     Map<String, Object> usage = superAdminService.getTenantApiUsage(tenantId, days);
     return ResponseEntity.ok(usage);
 }

 //
// Set custom rate limits for tenant
  
 @PostMapping("/tenants/{tenantId}/set-rate-limit")
 public ResponseEntity<Map<String, String>> setCustomRateLimit(
         @PathVariable Long tenantId,
         @RequestParam int requestsPerHour) {
     superAdminService.setCustomRateLimit(tenantId, requestsPerHour);
     return ResponseEntity.ok(Map.of("message", "Rate limit updated"));
 }

 //
// Temporarily throttle tenant
  
 @PostMapping("/tenants/{tenantId}/throttle")
 public ResponseEntity<Map<String, String>> throttleTenant(
         @PathVariable Long tenantId,
         @RequestParam int percentage,
         @RequestParam int durationMinutes) {
     superAdminService.throttleTenant(tenantId, percentage, durationMinutes);
     return ResponseEntity.ok(Map.of("message", "Tenant throttled successfully"));
 }

 // ========================================
 // ADD THESE IMPORTS AT THE TOP OF SuperAdminController.java
 // ========================================
 // import org.springframework.core.io.Resource;
 // import org.springframework.format.annotation.DateTimeFormat;
 // import org.springframework.http.HttpHeaders;
}