package com.saas.platform.service;

import com.saas.platform.dto.PasswordChangeRequest;
import com.saas.platform.dto.UpdateProfileRequest;
import com.saas.platform.model.Tenant;
import com.saas.platform.model.User;
import com.saas.platform.model.Notification;
import com.saas.platform.model.NotificationType;
import com.saas.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService - VERIFIED VERSION WITH NOTIFICATIONS
 * 
 * This version has extensive logging to help debug notification issues.
 * Replace your current UserService with this one.
 */
@Service
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final ActivityLogService activityLogService;
  
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Constructor - Verify all dependencies are injected
     */
    public UserService(UserRepository userRepository, 
                      TenantService tenantService,
                      ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.activityLogService = activityLogService;

        
      
    }
    
    /**
     * Create User - WITH EXTENSIVE LOGGING
     */
    @Transactional
    public User createUser(User user, Long tenantId) {
        log.info("═══════════════════════════════════════");
        log.info("🔹 USER CREATION STARTED");
        log.info("  Email: {}", user.getEmail());
        log.info("  Tenant ID: {}", tenantId);
        log.info("═══════════════════════════════════════");
        
        // Step 1: Validate email
        log.info("Step 1: Checking if email exists...");
        if (userRepository.existsByEmail(user.getEmail())) {
            log.error("❌ Email already exists: {}", user.getEmail());
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        log.info("✅ Email is unique");
        
        // Step 2: Get tenant
        log.info("Step 2: Loading tenant...");
        Tenant tenant = tenantService.getTenantById(tenantId);
        user.setTenant(tenant);
        log.info("✅ Tenant loaded: {}", tenant.getName());
        
        // Step 3: Set defaults
        if (user.getActive() == null) {
            user.setActive(true);
        }
        
        // Step 4: Encode password
        log.info("Step 3: Encoding password...");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        log.info("✅ Password encoded");
        
        // Step 5: Save user
        log.info("Step 4: Saving user to database...");
        User savedUser = userRepository.save(user);
        log.info("✅ User saved with ID: {}", savedUser.getId());
        
        // Step 6: Log activity
        log.info("Step 5: Logging activity...");
        try {
            activityLogService.logActivity(
                tenantId,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName() + " " + savedUser.getLastName(),
                "User created: " + savedUser.getEmail(),
                "user",
                "New user added to tenant"
            );
            log.info("✅ Activity logged");
        } catch (Exception e) {
            log.error("❌ Failed to log activity: {}", e.getMessage());
        }
        
     
        log.info("═══════════════════════════════════════");
        log.info("🔹 USER CREATION COMPLETED");
        log.info("  User ID: {}", savedUser.getId());
        log.info("  Email: {}", savedUser.getEmail());
        log.info("═══════════════════════════════════════");
        
        return savedUser;
    }
    
    // Keep all other methods unchanged...
    
    public List<User> getUsersByTenant(Long tenantId) {
        return userRepository.findByTenantId(tenantId);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
    
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setRole(userDetails.getRole());
        user.setActive(userDetails.getActive());
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest request) {
        User user = getUserById(id);
        
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
    
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        User user = getUserById(userId);
        
        if (!verifyPassword(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}