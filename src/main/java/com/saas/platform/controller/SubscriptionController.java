package com.saas.platform.controller;

import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SubscriptionController with proper permission structure:
 * - TENANT_OWNER: Full management access (create/upgrade/cancel)
 * - TENANT_ADMIN: View only
 * - SUPER_ADMIN: View only (for support)
 * - USER & VIEWER: No access
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final RoleValidator roleValidator;

    public SubscriptionController(SubscriptionService subscriptionService, 
                                 RoleValidator roleValidator) {
        this.subscriptionService = subscriptionService;
        this.roleValidator = roleValidator;
    }

    /**
     * Get tenant's subscription
     * VIEW ACCESS: TENANT_OWNER, TENANT_ADMIN, SUPER_ADMIN
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<?> getSubscription(@PathVariable Long tenantId) {
        try {
            // Check VIEW permission (TENANT_OWNER, TENANT_ADMIN, SUPER_ADMIN)
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
    public ResponseEntity<?> getPlans() {
        try {
            // Check if user has VIEW permission
            if (!roleValidator.hasSubscriptionViewPermission()) {
                throw new SecurityException("Access denied: Insufficient permissions to view plans");
            }
            return ResponseEntity.ok(SubscriptionPlan.values());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Change subscription plan
     * MANAGEMENT ACCESS: TENANT_OWNER ONLY
     */
    @PostMapping("/{tenantId}/change-plan")
    public ResponseEntity<?> changePlan(
            @PathVariable Long tenantId,
            @RequestParam SubscriptionPlan plan) {
        try {
            // Check MANAGEMENT permission (TENANT_OWNER only)
            roleValidator.requireSubscriptionPermission(tenantId);
            
            Subscription updated = subscriptionService.changePlan(tenantId, plan);
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
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
            // Check MANAGEMENT permission (TENANT_OWNER only)
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
            // Check VIEW permission
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