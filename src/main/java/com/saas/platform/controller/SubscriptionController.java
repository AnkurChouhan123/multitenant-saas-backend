package com.saas.platform.controller;

import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SubscriptionController - REST API for subscription management
 */
@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }
    
    /**
     * GET /api/subscriptions/tenant/{tenantId} - Get tenant's subscription
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<Subscription> getSubscription(@PathVariable Long tenantId) {
        try {
            Subscription subscription = subscriptionService.getSubscriptionByTenantId(tenantId);
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            // If subscription doesn't exist, create a trial one
            Subscription newSubscription = subscriptionService.createTrialSubscription(tenantId);
            return ResponseEntity.ok(newSubscription);
        }
    }
    
    /**
     * GET /api/subscriptions/plans - Get all available plans
     */
    @GetMapping("/plans")
    public ResponseEntity<SubscriptionPlan[]> getPlans() {
        return ResponseEntity.ok(SubscriptionPlan.values());
    }
    
    /**
     * POST /api/subscriptions/{tenantId}/upgrade - Change subscription plan
     */
    @PostMapping("/{tenantId}/change-plan")
    public ResponseEntity<Subscription> changePlan(
            @PathVariable Long tenantId,
            @RequestParam SubscriptionPlan plan) {
        Subscription updated = subscriptionService.changePlan(tenantId, plan);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * POST /api/subscriptions/{tenantId}/cancel - Cancel subscription
     */
    @PostMapping("/{tenantId}/cancel")
    public ResponseEntity<String> cancelSubscription(@PathVariable Long tenantId) {
        subscriptionService.cancelSubscription(tenantId);
        return ResponseEntity.ok("Subscription cancelled successfully");
    }
    
    /**
     * GET /api/subscriptions/{tenantId}/valid - Check if subscription is valid
     */
    @GetMapping("/{tenantId}/valid")
    public ResponseEntity<Boolean> isSubscriptionValid(@PathVariable Long tenantId) {
        boolean isValid = subscriptionService.isSubscriptionValid(tenantId);
        return ResponseEntity.ok(isValid);
    }
}