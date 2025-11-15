package com.saas.platform.service;

import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.model.Tenant;
import com.saas.platform.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SubscriptionService - Business logic for subscription management
 */
@Service
public class SubscriptionService {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final int TRIAL_DAYS = 14;
    
    private final SubscriptionRepository subscriptionRepository;
    private final TenantService tenantService;
    
    public SubscriptionService(SubscriptionRepository subscriptionRepository, 
                              TenantService tenantService) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantService = tenantService;
    }
    
    /**
     * Create FREE trial subscription for new tenant
     */
    @Transactional
    public Subscription createTrialSubscription(Long tenantId) {
        log.info("Creating trial subscription for tenant ID: {}", tenantId);
        
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        Subscription subscription = new Subscription();
        subscription.setTenant(tenant);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(TRIAL_DAYS));
        subscription.setIsActive(true);
        subscription.setAutoRenew(false);
        subscription.setCurrentUsers(1); // Admin user
        subscription.setCurrentApiCalls(0);
        
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Trial subscription created with ID: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Get subscription by tenant ID
     */
    public Subscription getSubscriptionByTenantId(Long tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No subscription found for tenant ID: " + tenantId));
    }
    
    /**
     * Upgrade/Downgrade subscription plan
     */
    @Transactional
    public Subscription changePlan(Long tenantId, SubscriptionPlan newPlan) {
        log.info("Changing plan for tenant ID: {} to {}", tenantId, newPlan);
        
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        
        // Check if downgrade is possible (user/API limits)
        if (isDowngrade(subscription.getPlan(), newPlan)) {
            validateDowngrade(subscription, newPlan);
        }
        
        subscription.setPlan(newPlan);
        
        // Extend end date for paid plans
        if (newPlan != SubscriptionPlan.FREE) {
            subscription.setEndDate(LocalDateTime.now().plusMonths(1));
            subscription.setAutoRenew(true);
        }
        
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Plan changed successfully");
        
        return updated;
    }
    
    /**
     * Cancel subscription
     */
    @Transactional
    public void cancelSubscription(Long tenantId) {
        log.info("Cancelling subscription for tenant ID: {}", tenantId);
        
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        subscription.setIsActive(false);
        subscription.setAutoRenew(false);
        
        subscriptionRepository.save(subscription);
        log.info("Subscription cancelled");
    }
    
    /**
     * Increment user count
     */
    @Transactional
    public void incrementUserCount(Long tenantId) {
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        
        if (subscription.hasReachedUserLimit()) {
            throw new IllegalStateException(
                    "User limit reached. Please upgrade your plan.");
        }
        
        subscription.setCurrentUsers(subscription.getCurrentUsers() + 1);
        subscriptionRepository.save(subscription);
    }
    
    /**
     * Increment API call count
     */
    @Transactional
    public void incrementApiCallCount(Long tenantId) {
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        
        if (subscription.hasReachedApiLimit()) {
            throw new IllegalStateException(
                    "API call limit reached. Please upgrade your plan.");
        }
        
        subscription.setCurrentApiCalls(subscription.getCurrentApiCalls() + 1);
        subscriptionRepository.save(subscription);
    }
    
    /**
     * Check if subscription is valid
     */
    public boolean isSubscriptionValid(Long tenantId) {
        try {
            Subscription subscription = getSubscriptionByTenantId(tenantId);
            return subscription.getIsActive() && !subscription.isExpired();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    // Helper methods
    
    private boolean isDowngrade(SubscriptionPlan current, SubscriptionPlan newPlan) {
        return current.getMonthlyPrice() > newPlan.getMonthlyPrice();
    }
    
    private void validateDowngrade(Subscription subscription, SubscriptionPlan newPlan) {
        if (!newPlan.isUnlimited()) {
            if (subscription.getCurrentUsers() > newPlan.getMaxUsers()) {
                throw new IllegalStateException(
                        "Cannot downgrade: Current users exceed new plan limit");
            }
        }
    }
}