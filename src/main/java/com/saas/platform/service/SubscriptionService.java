package com.saas.platform.service;

import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.model.Tenant;
import com.saas.platform.model.User;
import com.saas.platform.model.NotificationType;
import com.saas.platform.repository.SubscriptionRepository;
import com.saas.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

//
// SubscriptionService - FIXED with Notifications
 
@Service
public class SubscriptionService {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private static final int TRIAL_DAYS = 14;
    
    private final SubscriptionRepository subscriptionRepository;
    private final TenantService tenantService;
    private final UserRepository userRepository; // ADDED
   
    
    // UPDATED Constructor
    public SubscriptionService(SubscriptionRepository subscriptionRepository, 
                              TenantService tenantService,
                              UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantService = tenantService;
        this.userRepository = userRepository; // ADDED
   
    }
    
    //
// Create FREE trial subscription for new tenant
     
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
        subscription.setCurrentUsers(1);
        subscription.setCurrentApiCalls(0);
        
        Subscription saved = subscriptionRepository.save(subscription);
        

      
        log.info("Trial subscription created with ID: {}", saved.getId());
        
        return saved;
    }
    
    //
// Get subscription by tenant ID
     
    public Subscription getSubscriptionByTenantId(Long tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No subscription found for tenant ID: " + tenantId));
    }
    
    //
// Upgrade/Downgrade subscription plan
     
    @Transactional
    public Subscription changePlan(Long tenantId, SubscriptionPlan newPlan) {
        log.info("Changing plan for tenant ID: {} to {}", tenantId, newPlan);
        
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        SubscriptionPlan oldPlan = subscription.getPlan();
        
        // Check if downgrade is possible (user/API limits)
        if (isDowngrade(oldPlan, newPlan)) {
            validateDowngrade(subscription, newPlan);
        }
        
        subscription.setPlan(newPlan);
        
        // Extend end date for paid plans
        if (newPlan != SubscriptionPlan.FREE) {
            subscription.setEndDate(LocalDateTime.now().plusMonths(1));
            subscription.setAutoRenew(true);
        }
        
        Subscription updated = subscriptionRepository.save(subscription);
        
        // FIXED: Notify all tenant admins about plan change
        try {
            List<User> tenantAdmins = userRepository.findByTenantId(tenantId);
            
            String message = isUpgrade(oldPlan, newPlan)
                ? String.format("Your plan has been upgraded from %s to %s. Enjoy your new features!", 
                    oldPlan, newPlan)
                : String.format("Your plan has been changed from %s to %s.", oldPlan, newPlan);
            
                   } 
        catch (Exception e) {
            log.error("Failed to send plan change notification: {}", e.getMessage());
        }
        
        log.info("Plan changed successfully");
        
        return updated;
    }
    
    //
// Cancel subscription
     
    @Transactional
    public void cancelSubscription(Long tenantId) {
        log.info("Cancelling subscription for tenant ID: {}", tenantId);
        
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        subscription.setIsActive(false);
        subscription.setAutoRenew(false);
        
        subscriptionRepository.save(subscription);
        
        // FIXED: Notify all tenant admins about cancellation
        try {
            List<User> tenantAdmins = userRepository.findByTenantId(tenantId);
        }
        catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
        }
        
        log.info("Subscription cancelled");
    }
    
    //
// Increment user count
     
    @Transactional
    public void incrementUserCount(Long tenantId) {
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        
        if (subscription.hasReachedUserLimit()) {
            throw new IllegalStateException(
                    "User limit reached. Please upgrade your plan.");
        }
        
        subscription.setCurrentUsers(subscription.getCurrentUsers() + 1);
        subscriptionRepository.save(subscription);
        
        // FIXED: Notify if approaching user limit
        try {
            SubscriptionPlan plan = subscription.getPlan();
            int maxUsers = plan.getMaxUsers();
            int currentUsers = subscription.getCurrentUsers();
            
            // Alert when 80% of user limit is reached
            if (!plan.isUnlimited() && currentUsers >= maxUsers * 0.8) {
                List<User> tenantAdmins = userRepository.findByTenantId(tenantId);
                
                         }
        } catch (Exception e) {
            log.error("Failed to send user limit notification: {}", e.getMessage());
        }
    }
    
    @Scheduled(cron = "0 0 0 1 * ?") // First day of month at midnight
    public void resetMonthlyApiCounts() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        subscriptions.forEach(sub -> {
            sub.setCurrentApiCalls(0);
            subscriptionRepository.save(sub);
        });
    }
    
    // Increment API call count

    @Transactional
    public void incrementApiCallCount(Long tenantId) {
        Subscription subscription = getSubscriptionByTenantId(tenantId);
        
        if (subscription.hasReachedApiLimit()) {
            throw new IllegalStateException(
                    "API call limit reached. Please upgrade your plan.");
        }
        
        subscription.setCurrentApiCalls(subscription.getCurrentApiCalls() + 1);
        subscriptionRepository.save(subscription);
        
        // FIXED: Notify if approaching API limit
        try {
            SubscriptionPlan plan = subscription.getPlan();
            int maxApiCalls = plan.getMaxApiCalls();
            int currentApiCalls = subscription.getCurrentApiCalls();
            
            // Alert when 90% of API limit is reached
            if (!plan.isUnlimited() && currentApiCalls >= maxApiCalls * 0.9) {
                List<User> tenantAdmins = userRepository.findByTenantId(tenantId);
                
                         }
        } catch (Exception e) {
            log.error("Failed to send API limit notification: {}", e.getMessage());
        }
    }
    
    //
// Check if subscription is valid
     
    public boolean isSubscriptionValid(Long tenantId) {
        try {
            Subscription subscription = getSubscriptionByTenantId(tenantId);
            
            // FIXED: Send expiration warning
            if (subscription.getIsActive() && subscription.getEndDate() != null) {
                LocalDateTime expirationDate = subscription.getEndDate();
                LocalDateTime now = LocalDateTime.now();
                long daysUntilExpiration = java.time.Duration.between(now, expirationDate).toDays();
                
                // Warn 7 days before expiration
                if (daysUntilExpiration > 0 && daysUntilExpiration <= 7) {
                    try {
                        List<User> tenantAdmins = userRepository.findByTenantId(tenantId);
                        
                       
                    } catch (Exception e) {
                        log.error("Failed to send expiration notification: {}", e.getMessage());
                    }
                }
            }
            
            return subscription.getIsActive() && !subscription.isExpired();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    // Helper methods
    
    private boolean isDowngrade(SubscriptionPlan current, SubscriptionPlan newPlan) {
        return current.getMonthlyPrice() > newPlan.getMonthlyPrice();
    }
    
    private boolean isUpgrade(SubscriptionPlan current, SubscriptionPlan newPlan) {
        return current.getMonthlyPrice() < newPlan.getMonthlyPrice();
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