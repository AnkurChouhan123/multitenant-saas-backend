package com.saas.platform.controller;

import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final RoleValidator roleValidator;

    public SubscriptionController(SubscriptionService subscriptionService, RoleValidator roleValidator) {
		super();
		this.subscriptionService = subscriptionService;
		this.roleValidator = roleValidator;
	}

	
    
     //Get tenant's subscription
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
    
//    Get all available plans
    @GetMapping("/plans")
    public ResponseEntity<SubscriptionPlan[]> getPlans() {
        return ResponseEntity.ok(SubscriptionPlan.values());
    }
    
//    upgrade - Change subscription plan
    @PostMapping("/{tenantId}/change-plan")
    public ResponseEntity<Subscription> changePlan(
            @PathVariable Long tenantId,
            @RequestParam SubscriptionPlan plan) {
    	roleValidator.requireSubscriptionPermission(tenantId);
        Subscription updated = subscriptionService.changePlan(tenantId, plan);
        return ResponseEntity.ok(updated);
    }
    
//    Cancel subscription
    @PostMapping("/{tenantId}/cancel")
    public ResponseEntity<String> cancelSubscription(@PathVariable Long tenantId) {
        subscriptionService.cancelSubscription(tenantId);
        return ResponseEntity.ok("Subscription cancelled successfully");
    }
    
//    Check if subscription is valid
    @GetMapping("/{tenantId}/valid")
    public ResponseEntity<Boolean> isSubscriptionValid(@PathVariable Long tenantId) {
        boolean isValid = subscriptionService.isSubscriptionValid(tenantId);
        return ResponseEntity.ok(isValid);
    }
}