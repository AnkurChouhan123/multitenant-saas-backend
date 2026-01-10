package com.saas.platform.config;

import com.saas.platform.model.Plan;
import com.saas.platform.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * StartupPlansConfig - Auto-create default plans on startup
 * This ensures that FREE, BASIC, PRO, and ENTERPRISE plans exist in database
 */
@Configuration
@Order(1) // Run before super admin creation
public class StartupPlansConfig implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(StartupPlansConfig.class);
    
    private final PlanRepository planRepository;
    
    public StartupPlansConfig(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }
    
    @Override
    public void run(String... args) {
        log.info("═══════════════════════════════════════");
        log.info("🔧 INITIALIZING DEFAULT SUBSCRIPTION PLANS");
        log.info("═══════════════════════════════════════");
        
        // Create FREE plan
        createPlanIfNotExists(
            "FREE",
            0.0,
            5,
            1000,
            1,
            "Free tier with basic features",
            "• Up to 5 users\n• 1,000 API calls/month\n• 1 GB storage\n• Community support"
        );
        
        // Create BASIC plan
        createPlanIfNotExists(
            "BASIC",
            29.99,
            25,
            10000,
            10,
            "Perfect for small teams",
            "• Up to 25 users\n• 10,000 API calls/month\n• 10 GB storage\n• Email support\n• Basic analytics"
        );
        
        // Create PRO plan
        createPlanIfNotExists(
            "PRO",
            99.99,
            100,
            50000,
            50,
            "For growing businesses",
            "• Up to 100 users\n• 50,000 API calls/month\n• 50 GB storage\n• Priority support\n• Advanced analytics\n• Custom branding"
        );
        
        // Create ENTERPRISE plan
        createPlanIfNotExists(
            "ENTERPRISE",
            299.99,
            -1,  // Unlimited users
            -1,  // Unlimited API calls
            -1,  // Unlimited storage
            "Unlimited everything for large organizations",
            "• Unlimited users\n• Unlimited API calls\n• Unlimited storage\n• 24/7 phone support\n• Custom integrations\n• SLA guarantee\n• Dedicated account manager"
        );
        
        log.info("═══════════════════════════════════════");
        log.info("✅ PLAN INITIALIZATION COMPLETE");
        log.info("═══════════════════════════════════════");
    }
    
    private void createPlanIfNotExists(String name, Double monthlyPrice, 
                                      Integer maxUsers, Integer maxApiCalls, 
                                      Integer maxStorageGB, String description,
                                      String features) {
        if (planRepository.existsByName(name)) {
            log.info("✓ Plan '{}' already exists, skipping creation", name);
            return;
        }
        
        log.info("Creating plan: {}", name);
        
        Plan plan = new Plan();
        plan.setName(name);
        plan.setMonthlyPrice(monthlyPrice);
        plan.setMaxUsers(maxUsers);
        plan.setMaxApiCalls(maxApiCalls);
        plan.setMaxStorageGB(maxStorageGB);
        plan.setDescription(description);
        plan.setFeatures(features);
        plan.setIsActive(true);
        plan.setIsCustom(false);
        
        planRepository.save(plan);
        
        log.info("✅ Plan '{}' created successfully", name);
        log.info("   Price: ${}/month", monthlyPrice);
        log.info("   Users: {}", maxUsers == -1 ? "Unlimited" : maxUsers);
        log.info("   API Calls: {}", maxApiCalls == -1 ? "Unlimited" : maxApiCalls);
        log.info("   Storage: {} GB", maxStorageGB == -1 ? "Unlimited" : maxStorageGB);
    }
}