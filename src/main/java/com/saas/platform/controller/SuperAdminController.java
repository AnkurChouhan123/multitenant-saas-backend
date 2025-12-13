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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SuperAdminController - Platform Management Only
 * 
 * SUPER ADMIN manages the PLATFORM, not tenant internals.
 * - Tenant management (create, suspend, activate, delete)
 * - Subscription plans (create, modify, assign)
 * - Global analytics (aggregates only)
 * - Platform configuration
 * - Security & compliance
 * - Integrations
 * 
 * ❌ DOES NOT manage:
 * - Tenant users
 * - Tenant settings
 * - Tenant data/files
 * - Tenant webhooks
 * - Tenant activity logs
 */
@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
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
    
    /**
     * Get platform-wide statistics (aggregates only)
     */
    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDto> getPlatformStats() {
        PlatformStatsDto stats = superAdminService.getPlatformStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get platform health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getPlatformHealth() {
        Map<String, Object> health = superAdminService.getPlatformHealth();
        return ResponseEntity.ok(health);
    }
    
    // ========================================
    // TENANT MANAGEMENT
    // ========================================
    
    /**
     * Get all tenants (metadata only - no internal data)
     */
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantManagementDto>> getAllTenants() {
        List<TenantManagementDto> tenants = superAdminService.getAllTenantsForManagement();
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * Get single tenant metadata
     */
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantManagementDto> getTenantMetadata(@PathVariable Long tenantId) {
        TenantManagementDto tenant = superAdminService.getTenantMetadata(tenantId);
        return ResponseEntity.ok(tenant);
    }
    
    /**
     * Create new tenant
     */
    @PostMapping("/tenants")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant created = superAdminService.createTenant(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Suspend tenant (stops access but preserves data)
     */
    @PutMapping("/tenants/{tenantId}/suspend")
    public ResponseEntity<Map<String, String>> suspendTenant(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String reason) {
        superAdminService.suspendTenant(tenantId, reason);
        return ResponseEntity.ok(Map.of("message", "Tenant suspended successfully"));
    }
    
    /**
     * Activate tenant (resume access)
     */
    @PutMapping("/tenants/{tenantId}/activate")
    public ResponseEntity<Map<String, String>> activateTenant(@PathVariable Long tenantId) {
        superAdminService.activateTenant(tenantId);
        return ResponseEntity.ok(Map.of("message", "Tenant activated successfully"));
    }
    
    /**
     * Force logout all users of a tenant
     */
    @PostMapping("/tenants/{tenantId}/force-logout")
    public ResponseEntity<Map<String, String>> forceLogoutTenant(@PathVariable Long tenantId) {
        superAdminService.forceLogoutAllUsers(tenantId);
        return ResponseEntity.ok(Map.of("message", "All users logged out"));
    }
    
    /**
     * Soft delete tenant
     */
    @DeleteMapping("/tenants/{tenantId}")
    public ResponseEntity<Map<String, String>> deleteTenant(@PathVariable Long tenantId) {
        superAdminService.softDeleteTenant(tenantId);
        return ResponseEntity.ok(Map.of("message", "Tenant deleted successfully"));
    }
    
    /**
     * Impersonate tenant owner (generate temporary token)
     */
    @PostMapping("/tenants/{tenantId}/impersonate")
    public ResponseEntity<Map<String, String>> impersonateTenantOwner(@PathVariable Long tenantId) {
        String token = superAdminService.impersonateTenantOwner(tenantId);
        return ResponseEntity.ok(Map.of("token", token, "message", "Impersonation token generated"));
    }
    
    // ========================================
    // SUBSCRIPTION PLAN MANAGEMENT
    // ========================================
    
    /**
     * Get all subscription plans
     */
    @GetMapping("/plans")
    public ResponseEntity<List<Map<String, Object>>> getAllPlans() {
        List<Map<String, Object>> plans = superAdminService.getAllPlansWithStats();
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Create new subscription plan
     */
    @PostMapping("/plans")
    public ResponseEntity<Map<String, String>> createPlan(@RequestBody Map<String, Object> planData) {
        superAdminService.createSubscriptionPlan(planData);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Plan created successfully"));
    }
    
    /**
     * Update plan pricing/limits
     */
    @PutMapping("/plans/{planName}")
    public ResponseEntity<Map<String, String>> updatePlan(
            @PathVariable String planName,
            @RequestBody Map<String, Object> planData) {
        superAdminService.updatePlanPricing(planName, planData);
        return ResponseEntity.ok(Map.of("message", "Plan updated successfully"));
    }
    
    /**
     * Assign plan to tenant
     */
    @PostMapping("/tenants/{tenantId}/assign-plan")
    public ResponseEntity<Map<String, String>> assignPlan(
            @PathVariable Long tenantId,
            @RequestParam String planName) {
        superAdminService.assignPlanToTenant(tenantId, planName);
        return ResponseEntity.ok(Map.of("message", "Plan assigned successfully"));
    }
    
    /**
     * Get all subscriptions across platform
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, Object>>> getAllSubscriptions() {
        List<Map<String, Object>> subscriptions = superAdminService.getAllSubscriptionsWithRevenue();
        return ResponseEntity.ok(subscriptions);
    }
    
    // ========================================
    // GLOBAL ANALYTICS (AGGREGATES ONLY)
    // ========================================
    
    /**
     * Get global analytics (no tenant-specific data)
     */
    @GetMapping("/analytics/global")
    public ResponseEntity<Map<String, Object>> getGlobalAnalytics() {
        Map<String, Object> analytics = superAdminService.getGlobalAnalytics();
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get platform usage statistics
     */
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
    
    /**
     * Get revenue analytics
     */
    @GetMapping("/analytics/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueAnalytics() {
        Map<String, Object> revenue = superAdminService.getRevenueAnalytics();
        return ResponseEntity.ok(revenue);
    }
    
    // ========================================
    // SECURITY & COMPLIANCE
    // ========================================
    
    /**
     * Get global audit logs (platform-level only)
     */
    @GetMapping("/security/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getGlobalAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> logs = superAdminService.getGlobalAuditLogs(page, size);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get security alerts
     */
    @GetMapping("/security/alerts")
    public ResponseEntity<List<Map<String, Object>>> getSecurityAlerts() {
        List<Map<String, Object>> alerts = superAdminService.getSecurityAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Get login history across platform
     */
    @GetMapping("/security/login-history")
    public ResponseEntity<Map<String, Object>> getGlobalLoginHistory() {
        Map<String, Object> history = superAdminService.getGlobalLoginHistory();
        return ResponseEntity.ok(history);
    }
    
    /**
     * Force password reset for any user
     */
    @PostMapping("/security/force-password-reset")
    public ResponseEntity<Map<String, String>> forcePasswordReset(@RequestParam String email) {
        superAdminService.forcePasswordReset(email);
        return ResponseEntity.ok(Map.of("message", "Password reset email sent"));
    }
    
    /**
     * Disable compromised account
     */
    @PostMapping("/security/disable-account")
    public ResponseEntity<Map<String, String>> disableCompromisedAccount(@RequestParam Long userId) {
        superAdminService.disableAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account disabled"));
    }
    
    // ========================================
    // PLATFORM CONFIGURATION
    // ========================================
    
    /**
     * Get platform configuration
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPlatformConfig() {
        Map<String, Object> config = superAdminService.getPlatformConfiguration();
        return ResponseEntity.ok(config);
    }
    
    /**
     * Update platform configuration
     */
    @PutMapping("/config")
    public ResponseEntity<Map<String, String>> updatePlatformConfig(
            @RequestBody Map<String, Object> config) {
        superAdminService.updatePlatformConfiguration(config);
        return ResponseEntity.ok(Map.of("message", "Configuration updated"));
    }
    
    /**
     * Toggle maintenance mode
     */
    @PostMapping("/config/maintenance-mode")
    public ResponseEntity<Map<String, String>> toggleMaintenanceMode(@RequestParam boolean enabled) {
        superAdminService.setMaintenanceMode(enabled);
        return ResponseEntity.ok(Map.of(
            "message", enabled ? "Maintenance mode enabled" : "Maintenance mode disabled"
        ));
    }
    
    /**
     * Update feature flags
     */
    @PutMapping("/config/feature-flags")
    public ResponseEntity<Map<String, String>> updateFeatureFlags(
            @RequestBody Map<String, Boolean> flags) {
        superAdminService.updateFeatureFlags(flags);
        return ResponseEntity.ok(Map.of("message", "Feature flags updated"));
    }
    
    // ========================================
    // PLATFORM INTEGRATIONS
    // ========================================
    
    /**
     * Get platform integrations status
     */
    @GetMapping("/integrations")
    public ResponseEntity<Map<String, Object>> getIntegrations() {
        Map<String, Object> integrations = superAdminService.getIntegrationStatus();
        return ResponseEntity.ok(integrations);
    }
    
    /**
     * Update payment gateway configuration
     */
    @PutMapping("/integrations/payment")
    public ResponseEntity<Map<String, String>> updatePaymentGateway(
            @RequestBody Map<String, String> config) {
        superAdminService.updatePaymentGatewayConfig(config);
        return ResponseEntity.ok(Map.of("message", "Payment gateway updated"));
    }
    
    /**
     * Update email provider configuration
     */
    @PutMapping("/integrations/email")
    public ResponseEntity<Map<String, String>> updateEmailProvider(
            @RequestBody Map<String, String> config) {
        superAdminService.updateEmailProviderConfig(config);
        return ResponseEntity.ok(Map.of("message", "Email provider updated"));
    }
    
    // ========================================
    // SYSTEM MONITORING & DEBUGGING
    // ========================================
    
    /**
     * Get system errors
     */
    @GetMapping("/monitoring/errors")
    public ResponseEntity<List<Map<String, Object>>> getSystemErrors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Map<String, Object>> errors = superAdminService.getSystemErrors(page, size);
        return ResponseEntity.ok(errors);
    }
    
    /**
     * Get backend logs
     */
    @GetMapping("/monitoring/logs")
    public ResponseEntity<List<String>> getBackendLogs(
            @RequestParam(defaultValue = "100") int lines) {
        List<String> logs = superAdminService.getRecentLogs(lines);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Retry failed jobs
     */
    @PostMapping("/monitoring/retry-jobs")
    public ResponseEntity<Map<String, String>> retryFailedJobs() {
        int retried = superAdminService.retryFailedJobs();
        return ResponseEntity.ok(Map.of("message", retried + " jobs retried"));
    }
}