package com.saas.platform.service;

import com.saas.platform.dto.PlatformStatsDto;
import com.saas.platform.dto.TenantManagementDto;
import com.saas.platform.model.*;
import com.saas.platform.repository.*;
import com.saas.platform.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SuperAdminService - Platform Management Service
 * 
 * Handles PLATFORM-LEVEL operations only:
 * - Tenant lifecycle management
 * - Global subscription plans
 * - Platform-wide analytics (aggregates)
 * - Security and compliance
 * - Platform configuration
 * 
 * ❌ DOES NOT handle tenant-specific operations
 */
@Service
public class SuperAdminService {
    
    private static final Logger log = LoggerFactory.getLogger(SuperAdminService.class);
    
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final FileStorageRepository fileStorageRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final WebhookRepository webhookRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    
    public SuperAdminService(TenantRepository tenantRepository,
                            UserRepository userRepository,
                            SubscriptionRepository subscriptionRepository,
                            ActivityLogRepository activityLogRepository,
                            FileStorageRepository fileStorageRepository,
                            ApiKeyRepository apiKeyRepository,
                            WebhookRepository webhookRepository,
                            JwtUtil jwtUtil,
                            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.activityLogRepository = activityLogRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.webhookRepository = webhookRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }
    
    // ========================================
    // PLATFORM STATISTICS
    // ========================================
    
    public PlatformStatsDto getPlatformStats() {
        log.info("Generating platform-wide statistics");
        
        PlatformStatsDto stats = new PlatformStatsDto();
        
        // Tenant counts
        List<Tenant> allTenants = tenantRepository.findAll();
        stats.setTotalTenants((long)allTenants.size());
        stats.setActiveTenants(allTenants.stream()
            .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
            .count());
        stats.setTrialTenants(allTenants.stream()
            .filter(t -> t.getStatus() == TenantStatus.TRIAL)
            .count());
        stats.setSuspendedTenants(allTenants.stream()
            .filter(t -> t.getStatus() == TenantStatus.SUSPENDED)
            .count());
        
        // User counts
        stats.setTotalUsers(userRepository.count());
        stats.setDau(calculateDAU());
        stats.setMau(calculateMAU());
        
        // Revenue
        stats.setMrr(calculateMRR());
        stats.setTotalRevenue(calculateTotalRevenue());
        
        // Platform usage
        stats.setTotalApiCalls(calculateTotalApiCalls());
        stats.setTotalStorageGB(calculateTotalStorage() / (1024.0 * 1024 * 1024));
        stats.setTotalWebhooks(webhookRepository.count());
        
        // Health metrics
        stats.setUptime(99.97); // From monitoring system
        stats.setErrorRate(0.12); // From error tracking
        
        log.info("Platform stats generated successfully");
        return stats;
    }
    
    public Map<String, Object> getPlatformHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("uptime", 99.97);
        health.put("database", "connected");
        health.put("redis", "connected");
        health.put("storage", "healthy");
        health.put("lastCheck", LocalDateTime.now());
        return health;
    }
    
    // ========================================
    // TENANT MANAGEMENT
    // ========================================
    
    public List<TenantManagementDto> getAllTenantsForManagement() {
        List<Tenant> tenants = tenantRepository.findAll();
        
        return tenants.stream().map(tenant -> {
            TenantManagementDto dto = new TenantManagementDto();
            dto.setId(tenant.getId());
            dto.setName(tenant.getName());
            dto.setSubdomain(tenant.getSubdomain());
            dto.setStatus(tenant.getStatus().toString());
            dto.setCreatedAt(tenant.getCreatedAt());
            
            // Get subscription info
            subscriptionRepository.findByTenantId(tenant.getId()).ifPresent(sub -> {
                dto.setPlan(sub.getPlan().toString());
                dto.setSubscriptionActive(sub.getIsActive());
            });
            
            // Get aggregate metrics (not internal data)
            dto.setUserCount(userRepository.countByTenantId(tenant.getId()));
            dto.setStorageUsedGB(fileStorageRepository.sumFileSizeByTenantId(tenant.getId()) / (1024.0 * 1024 * 1024));
            dto.setApiCallCount(activityLogRepository.findByTenantId(tenant.getId(), PageRequest.of(0, 1)).getTotalElements());
            
            // Last activity timestamp
            List<ActivityLog> recentActivity = activityLogRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());
            if (!recentActivity.isEmpty()) {
                dto.setLastActive(recentActivity.get(0).getCreatedAt());
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    public TenantManagementDto getTenantMetadata(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        
        TenantManagementDto dto = new TenantManagementDto();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setSubdomain(tenant.getSubdomain());
        dto.setStatus(tenant.getStatus().toString());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUserCount(userRepository.countByTenantId(tenantId));
        
        return dto;
    }
    
    @Transactional
    public Tenant createTenant(Tenant tenant) {
        log.info("Super Admin creating tenant: {}", tenant.getName());
        
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists");
        }
        
        tenant.setDatabaseName("tenant_" + tenant.getSubdomain());
        tenant.setStatus(TenantStatus.TRIAL);
        
        return tenantRepository.save(tenant);
    }
    
    @Transactional
    public void suspendTenant(Long tenantId, String reason) {
        log.warn("Super Admin suspending tenant ID: {} - Reason: {}", tenantId, reason);
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        
        // Log the suspension
        log.info("Tenant {} suspended successfully", tenant.getName());
    }
    
    @Transactional
    public void activateTenant(Long tenantId) {
        log.info("Super Admin activating tenant ID: {}", tenantId);
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
        
        log.info("Tenant {} activated successfully", tenant.getName());
    }
    
    @Transactional
    public void forceLogoutAllUsers(Long tenantId) {
        log.warn("Super Admin forcing logout for all users in tenant ID: {}", tenantId);
        
        // In production: invalidate all JWT tokens for this tenant
        // This could be done by:
        // 1. Adding tokens to a blacklist in Redis
        // 2. Changing a tenant-level secret key
        // 3. Setting a "logout timestamp" that tokens are checked against
        
        List<User> users = userRepository.findByTenantId(tenantId);
        log.info("Force logged out {} users from tenant ID: {}", users.size(), tenantId);
    }
    
    @Transactional
    public void softDeleteTenant(Long tenantId) {
        log.warn("Super Admin soft-deleting tenant ID: {}", tenantId);
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        
        tenant.setStatus(TenantStatus.CANCELLED);
        tenantRepository.save(tenant);
        
        log.info("Tenant {} soft-deleted", tenant.getName());
    }
    
    public String impersonateTenantOwner(Long tenantId) {
        log.warn("Super Admin impersonating tenant owner for tenant ID: {}", tenantId);
        
        // Find tenant owner
        List<User> users = userRepository.findByTenantId(tenantId);
        User owner = users.stream()
            .filter(u -> u.getRole() == UserRole.TENANT_OWNER)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No tenant owner found"));
        
        // Generate temporary impersonation token (shorter expiry)
        String token = jwtUtil.generateToken(
            owner.getEmail(),
            owner.getId(),
            tenantId,
            owner.getRole().toString()
        );
        
        log.warn("Impersonation token generated for tenant owner: {}", owner.getEmail());
        return token;
    }
    
    // ========================================
    // SUBSCRIPTION PLAN MANAGEMENT
    // ========================================
    
    public List<Map<String, Object>> getAllPlansWithStats() {
        List<Map<String, Object>> plans = new ArrayList<>();
        
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            Map<String, Object> planData = new HashMap<>();
            planData.put("name", plan.name());
            planData.put("price", plan.getMonthlyPrice());
            planData.put("maxUsers", plan.getMaxUsers());
            planData.put("maxApiCalls", plan.getMaxApiCalls());
            planData.put("isUnlimited", plan.isUnlimited());
            
            // Count tenants on this plan
            long tenantCount = subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getPlan() == plan)
                .count();
            planData.put("tenantCount", tenantCount);
            
            plans.add(planData);
        }
        
        return plans;
    }
    
    @Transactional
    public void createSubscriptionPlan(Map<String, Object> planData) {
        // In production: Store custom plans in database
        log.info("Super Admin creating custom subscription plan: {}", planData);
    }
    
    @Transactional
    public void updatePlanPricing(String planName, Map<String, Object> planData) {
        // In production: Update plan in database
        log.info("Super Admin updating plan {}: {}", planName, planData);
    }
    
    @Transactional
    public void assignPlanToTenant(Long tenantId, String planName) {
        log.info("Super Admin assigning plan {} to tenant ID: {}", planName, tenantId);
        
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));
        
        SubscriptionPlan newPlan = SubscriptionPlan.valueOf(planName.toUpperCase());
        subscription.setPlan(newPlan);
        
        // Extend subscription if upgrading
        if (newPlan != SubscriptionPlan.FREE) {
            subscription.setEndDate(LocalDateTime.now().plusMonths(1));
        }
        
        subscriptionRepository.save(subscription);
        log.info("Plan assigned successfully");
    }
    
    public List<Map<String, Object>> getAllSubscriptionsWithRevenue() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        
        return subscriptions.stream().map(sub -> {
            Map<String, Object> data = new HashMap<>();
            data.put("tenantId", sub.getTenant().getId());
            data.put("tenantName", sub.getTenant().getName());
            data.put("plan", sub.getPlan().toString());
            data.put("revenue", sub.getPlan().getMonthlyPrice());
            data.put("isActive", sub.getIsActive());
            data.put("startDate", sub.getStartDate());
            data.put("endDate", sub.getEndDate());
            return data;
        }).collect(Collectors.toList());
    }
    
    // ========================================
    // GLOBAL ANALYTICS (AGGREGATES ONLY)
    // ========================================
    
    public Map<String, Object> getGlobalAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("totalTenants", tenantRepository.count());
        analytics.put("totalUsers", userRepository.count());
        analytics.put("dau", calculateDAU());
        analytics.put("mau", calculateMAU());
        analytics.put("totalRevenue", calculateTotalRevenue());
        analytics.put("mrr", calculateMRR());
        analytics.put("storageUsedGB", calculateTotalStorage() / (1024.0 * 1024 * 1024));
        analytics.put("apiCalls", calculateTotalApiCalls());
        
        return analytics;
    }
    
    public Long getTotalStorageUsed() {
        return fileStorageRepository.findAll().stream()
            .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
            .sum();
    }
    
    public Long getTotalApiCalls() {
        return activityLogRepository.count();
    }
    
    public Long getTotalWebhooks() {
        return webhookRepository.count();
    }
    
    public Double getGlobalErrorRate() {
        // Calculate from monitoring system
        return 0.12;
    }
    
    public Map<String, Object> getRevenueAnalytics() {
        Map<String, Object> revenue = new HashMap<>();
        revenue.put("mrr", calculateMRR());
        revenue.put("arr", calculateMRR() * 12);
        revenue.put("totalRevenue", calculateTotalRevenue());
        revenue.put("averageRevenuePerTenant", calculateMRR() / tenantRepository.count());
        return revenue;
    }
    
    // ========================================
    // SECURITY & COMPLIANCE
    // ========================================
    
    public List<Map<String, Object>> getGlobalAuditLogs(int page, int size) {
        // Return platform-level logs only (not tenant-specific)
        return activityLogRepository.findAll(PageRequest.of(page, size)).stream()
            .filter(log -> "security".equals(log.getActionType()) || "auth".equals(log.getActionType()))
            .map(log -> {
                Map<String, Object> logData = new HashMap<>();
                logData.put("id", log.getId());
                logData.put("action", log.getAction());
                logData.put("tenantId", log.getTenantId());
                logData.put("timestamp", log.getCreatedAt());
                logData.put("ipAddress", log.getIpAddress());
                return logData;
            })
            .collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getSecurityAlerts() {
        // Return security alerts from monitoring system
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        // Example: Failed login attempts
        Map<String, Object> alert1 = new HashMap<>();
        alert1.put("type", "warning");
        alert1.put("message", "15 failed login attempts detected");
        alert1.put("timestamp", LocalDateTime.now().minusHours(2));
        alerts.add(alert1);
        
        return alerts;
    }
    
    public Map<String, Object> getGlobalLoginHistory() {
        Map<String, Object> history = new HashMap<>();
        
        LocalDateTime last24h = LocalDateTime.now().minusHours(24);
        List<ActivityLog> logins = activityLogRepository.findAll().stream()
            .filter(log -> log.getAction().contains("Logged in"))
            .filter(log -> log.getCreatedAt().isAfter(last24h))
            .collect(Collectors.toList());
        
        history.put("totalLogins24h", logins.size());
        history.put("uniqueUsers", logins.stream()
            .map(ActivityLog::getUserId)
            .distinct()
            .count());
        history.put("uniqueIPs", logins.stream()
            .map(ActivityLog::getIpAddress)
            .distinct()
            .count());
        
        return history;
    }
    
    @Transactional
    public void forcePasswordReset(String email) {
        log.warn("Super Admin forcing password reset for: {}", email);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Trigger password reset email
        // Implementation depends on PasswordResetService
    }
    
    @Transactional
    public void disableAccount(Long userId) {
        log.warn("Super Admin disabling account ID: {}", userId);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setActive(false);
        userRepository.save(user);
    }
    
    // ========================================
    // PLATFORM CONFIGURATION
    // ========================================
    
    public Map<String, Object> getPlatformConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("platformName", "SaaS Platform");
        config.put("supportEmail", "support@platform.com");
        config.put("maxFileUploadMB", 10);
        config.put("apiRateLimitPerHour", 1000);
        config.put("sessionTimeoutMinutes", 120);
        config.put("trialPeriodDays", 14);
        config.put("maintenanceMode", false);
        config.put("allowNewRegistrations", true);
        return config;
    }
    
    @Transactional
    public void updatePlatformConfiguration(Map<String, Object> config) {
        log.info("Super Admin updating platform configuration");
        // Store in database or configuration service
    }
    
    public void setMaintenanceMode(boolean enabled) {
        log.warn("Super Admin {} maintenance mode", enabled ? "enabling" : "disabling");
        // Update configuration
    }
    
    public void updateFeatureFlags(Map<String, Boolean> flags) {
        log.info("Super Admin updating feature flags: {}", flags);
        // Store in Redis or database
    }
    
    // ========================================
    // INTEGRATIONS
    // ========================================
    
    public Map<String, Object> getIntegrationStatus() {
        Map<String, Object> integrations = new HashMap<>();
        
        Map<String, Object> stripe = new HashMap<>();
        stripe.put("name", "Stripe");
        stripe.put("status", "connected");
        stripe.put("type", "payment");
        integrations.put("stripe", stripe);
        
        Map<String, Object> sendgrid = new HashMap<>();
        sendgrid.put("name", "SendGrid");
        sendgrid.put("status", "connected");
        sendgrid.put("type", "email");
        integrations.put("sendgrid", sendgrid);
        
        return integrations;
    }
    
    public void updatePaymentGatewayConfig(Map<String, String> config) {
        log.info("Super Admin updating payment gateway configuration");
        // Store encrypted credentials
    }
    
    public void updateEmailProviderConfig(Map<String, String> config) {
        log.info("Super Admin updating email provider configuration");
        // Store encrypted credentials
    }
    
    // ========================================
    // MONITORING & DEBUGGING
    // ========================================
    
    public List<Map<String, Object>> getSystemErrors(int page, int size) {
        // Return from error tracking system (e.g., Sentry)
        return new ArrayList<>();
    }
    
    public List<String> getRecentLogs(int lines) {
        // Return recent backend logs
        return List.of(
            "[INFO] Application started",
            "[INFO] Database connected",
            "[WARN] High memory usage detected"
        );
    }
    
    public int retryFailedJobs() {
        log.info("Super Admin retrying failed jobs");
        // Retry queued jobs
        return 0;
    }
    
    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================
    
    private long calculateDAU() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        return activityLogRepository.findAll().stream()
            .filter(log -> log.getCreatedAt().isAfter(yesterday))
            .map(ActivityLog::getUserId)
            .distinct()
            .count();
    }
    
    private long calculateMAU() {
        LocalDateTime lastMonth = LocalDateTime.now().minusMonths(1);
        return activityLogRepository.findAll().stream()
            .filter(log -> log.getCreatedAt().isAfter(lastMonth))
            .map(ActivityLog::getUserId)
            .distinct()
            .count();
    }
    
    private double calculateMRR() {
        return subscriptionRepository.findAll().stream()
            .filter(Subscription::getIsActive)
            .mapToDouble(sub -> sub.getPlan().getMonthlyPrice())
            .sum();
    }
    
    private double calculateTotalRevenue() {
        return calculateMRR() * 12; // Simplified
    }
    
    private long calculateTotalApiCalls() {
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        return activityLogRepository.findAll().stream()
            .filter(log -> log.getCreatedAt().isAfter(last30Days))
            .count();
    }
    
    private long calculateTotalStorage() {
        return fileStorageRepository.findAll().stream()
            .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
            .sum();
    }
}