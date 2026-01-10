package com.saas.platform.controller;

import com.saas.platform.model.Plan;
import com.saas.platform.model.Subscription;
import com.saas.platform.repository.PlanRepository;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SubscriptionController with proper permission structure:
 * - TENANT_OWNER: Full management access (create/upgrade/cancel)
 * - TENANT_ADMIN: View only
 * - SUPER_ADMIN: View only (for support)
 * - USER & VIEWER: No access
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final RoleValidator roleValidator;
    private final PlanRepository planRepository;

    public SubscriptionController(SubscriptionService subscriptionService, 
                                 RoleValidator roleValidator, 
                                 PlanRepository planRepository) {
        this.subscriptionService = subscriptionService;
        this.roleValidator = roleValidator;
        this.planRepository = planRepository;
    }

    /**
     * Get tenant's subscription
     * VIEW ACCESS: TENANT_OWNER, TENANT_ADMIN, SUPER_ADMIN
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<?> getSubscription(@PathVariable Long tenantId) {
        try {
            roleValidator.requireSubscriptionViewPermission(tenantId);

            Subscription subscription = subscriptionService.getSubscriptionByTenantId(tenantId);
            return ResponseEntity.ok(subscription);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            // If subscription doesn't exist, create a trial one
            try {
                roleValidator.requireSubscriptionViewPermission(tenantId);
                Subscription newSubscription = subscriptionService.createTrialSubscription(tenantId);
                return ResponseEntity.ok(newSubscription);
            } catch (SecurityException se) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(se.getMessage()));
            }
        }
    }

    /**
     * Get all available plans
     * VIEW ACCESS: TENANT_OWNER, TENANT_ADMIN, SUPER_ADMIN
     */
    @GetMapping("/plans")
    @PreAuthorize("hasAnyAuthority('ROLE_TENANT_OWNER', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAvailablePlans() {
        try {
            List<Plan> activePlans = planRepository.findByIsActiveTrue();
            
            List<Map<String, Object>> plansResponse = activePlans.stream()
                .map(plan -> {
                    Map<String, Object> planData = new HashMap<>();
                    planData.put("id", plan.getId());
                    planData.put("name", plan.getName());
                    planData.put("price", plan.getMonthlyPrice());
                    planData.put("monthlyPrice", plan.getMonthlyPrice());
                    planData.put("description", plan.getDescription());
                    planData.put("maxUsers", plan.getMaxUsers());
                    planData.put("max_users", plan.getMaxUsers());
                    planData.put("maxApiCalls", plan.getMaxApiCalls());
                    planData.put("max_api_calls", plan.getMaxApiCalls());
                    planData.put("maxStorageGB", plan.getMaxStorageGB());
                    planData.put("maxStorage", plan.getMaxStorageGB());
                    planData.put("max_storage_gb", plan.getMaxStorageGB());
                    planData.put("features", plan.getFeatures());
                    planData.put("isActive", plan.getIsActive());
                    planData.put("isCustom", plan.getIsCustom());
                    return planData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(plansResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.emptyList());
        }
    }

    /**
     * Change subscription plan
     * MANAGEMENT ACCESS: TENANT_OWNER ONLY
     * Accepts either plan name or plan ID
     */
    @PostMapping("/{tenantId}/change-plan")
    public ResponseEntity<?> changePlan(
            @PathVariable Long tenantId, 
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) Long planId) {
        try {
            roleValidator.requireSubscriptionPermission(tenantId);

            Subscription updated;
            
            if (planId != null) {
                // Change by plan ID
                updated = subscriptionService.changePlanById(tenantId, planId);
            } else if (plan != null && !plan.isEmpty()) {
                // Change by plan name
                updated = subscriptionService.changePlan(tenantId, plan);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Either 'plan' or 'planId' parameter is required"));
            }
            
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Cancel subscription
     * MANAGEMENT ACCESS: TENANT_OWNER ONLY
     */
    @PostMapping("/{tenantId}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long tenantId) {
        try {
            roleValidator.requireSubscriptionPermission(tenantId);

            subscriptionService.cancelSubscription(tenantId);
            return ResponseEntity.ok(createSuccessResponse("Subscription cancelled successfully"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Check if subscription is valid
     * VIEW ACCESS: TENANT_OWNER, TENANT_ADMIN, SUPER_ADMIN
     */
    @GetMapping("/{tenantId}/valid")
    public ResponseEntity<?> isSubscriptionValid(@PathVariable Long tenantId) {
        try {
            roleValidator.requireSubscriptionViewPermission(tenantId);

            boolean isValid = subscriptionService.isSubscriptionValid(tenantId);
            return ResponseEntity.ok(isValid);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Check current user's subscription permissions
     * Returns what actions the user can perform
     */
    @GetMapping("/check-permission")
    public ResponseEntity<Map<String, Object>> checkPermission() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("canView", roleValidator.hasSubscriptionViewPermission());
            response.put("canManage", roleValidator.hasSubscriptionManagementPermission());
            response.put("role", roleValidator.getCurrentUser().getRole().toString());
            response.put("tenantId", roleValidator.getCurrentUser().getTenant().getId());
        } catch (Exception e) {
            response.put("canView", false);
            response.put("canManage", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // Helper methods

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }

    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        return response;
    }
}