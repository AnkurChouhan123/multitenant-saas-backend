package com.saas.platform.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.saas.platform.dto.TenantManagementDto;
import com.saas.platform.model.ActivityLog;
import com.saas.platform.model.Plan;
import com.saas.platform.model.Subscription;
import com.saas.platform.model.Tenant;
import com.saas.platform.model.TenantStatus;
import com.saas.platform.model.User;
import com.saas.platform.repository.ActivityLogRepository;
import com.saas.platform.repository.FileStorageRepository;
import com.saas.platform.repository.PlanRepository;
import com.saas.platform.repository.SubscriptionRepository;
import com.saas.platform.repository.TenantRepository;
import com.saas.platform.repository.UserRepository;

@Service
public class SuperAdminService {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminService.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ActivityLogRepository activityLogRepository;
    private final FileStorageRepository fileStorageRepository;
    private final PlanRepository planRepository;

    public SuperAdminService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            ActivityLogRepository activityLogRepository,
            FileStorageRepository fileStorageRepository,
            PlanRepository planRepository
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.activityLogRepository = activityLogRepository;
        this.fileStorageRepository = fileStorageRepository;
        this.planRepository = planRepository;
    }

    /* =========================
       TENANT MANAGEMENT
       ========================= */

    public List<TenantManagementDto> getAllTenantsForManagement() {
        return tenantRepository.findAll().stream().map(tenant -> {
            TenantManagementDto dto = new TenantManagementDto();
            dto.setId(tenant.getId());
            dto.setName(tenant.getName());
            dto.setSubdomain(tenant.getSubdomain());
            dto.setStatus(tenant.getStatus().name());
            dto.setCreatedAt(tenant.getCreatedAt());
            dto.setUserCount(userRepository.countByTenantId(tenant.getId()));
            dto.setStorageUsedGB(
                fileStorageRepository.sumFileSizeByTenantId(tenant.getId()) / (1024.0 * 1024 * 1024)
            );

            List<ActivityLog> logs =
                activityLogRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId());
            if (!logs.isEmpty()) {
                dto.setLastActive(logs.get(0).getCreatedAt());
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
        dto.setStatus(tenant.getStatus().name());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUserCount(userRepository.countByTenantId(tenantId));
        return dto;
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsBySubdomain(tenant.getSubdomain())) {
            throw new IllegalArgumentException("Subdomain already exists");
        }

        tenant.setDatabaseName("tenant_" + tenant.getSubdomain());
        tenant.setStatus(TenantStatus.TRIAL);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void suspendTenant(Long tenantId, String reason) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void activateTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void softDeleteTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        tenant.setStatus(TenantStatus.CANCELLED);
        tenantRepository.save(tenant);
    }

    @Transactional
    public void forceLogoutAllUsers(Long tenantId) {
        List<User> users = userRepository.findByTenantId(tenantId);
        log.info("Force logout triggered for {} users", users.size());
    }

    /* =========================
       PLAN & SUBSCRIPTION
       ========================= */

    public List<Map<String, Object>> getAllPlansWithStats() {
        return planRepository.findAll().stream().map(plan -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", plan.getId());
            data.put("name", plan.getName());
            data.put("price", plan.getMonthlyPrice());
            data.put("maxUsers", plan.getMaxUsers());
            data.put("maxApiCalls", plan.getMaxApiCalls());
            data.put("maxStorageGB", plan.getMaxStorageGB());
            data.put("isActive", plan.getIsActive());
            data.put("isCustom", plan.getIsCustom());
            return data;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Plan createSubscriptionPlan(Map<String, Object> planData) {
        if (planRepository.existsByName((String) planData.get("name"))) {
            throw new IllegalArgumentException("Plan already exists");
        }

        Plan plan = new Plan();
        plan.setName((String) planData.get("name"));
        plan.setMonthlyPrice(((Number) planData.get("monthlyPrice")).doubleValue());
        plan.setMaxUsers(((Number) planData.get("maxUsers")).intValue());
        plan.setMaxApiCalls(((Number) planData.get("maxApiCalls")).intValue());
        plan.setMaxStorageGB(((Number) planData.get("maxStorageGB")).intValue());
        plan.setDescription((String) planData.get("description"));
        plan.setIsCustom(true);
        plan.setIsActive(true);

        return planRepository.save(plan);
    }

    @Transactional
    public Plan updatePlanPricing(Long planId, Map<String, Object> planData) {
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (planData.containsKey("monthlyPrice")) {
            plan.setMonthlyPrice(((Number) planData.get("monthlyPrice")).doubleValue());
        }
        if (planData.containsKey("isActive")) {
            plan.setIsActive((Boolean) planData.get("isActive"));
        }

        return planRepository.save(plan);
    }

    @Transactional
    public void deletePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        planRepository.delete(plan);
    }

    @Transactional
    public void assignPlanToTenant(Long tenantId, Long planId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        if (!plan.getIsActive()) {
            throw new IllegalArgumentException("Plan is inactive");
        }

        subscription.setPlan(plan);
        subscription.setEndDate(LocalDateTime.now().plusMonths(1));
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void togglePlanStatus(Long planId) {
        Plan plan = planRepository.findById(planId)
            .orElseThrow(() -> new IllegalArgumentException("Plan not found"));
        plan.setIsActive(!plan.getIsActive());
        planRepository.save(plan);
    }
}
