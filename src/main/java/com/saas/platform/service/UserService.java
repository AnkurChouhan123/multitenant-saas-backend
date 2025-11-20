package com.saas.platform.service;

import com.saas.platform.dto.PasswordChangeRequest;
import com.saas.platform.dto.UpdateProfileRequest;
import com.saas.platform.model.Tenant;
import com.saas.platform.model.User;
import com.saas.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService - FIXED
 * Added updateProfile method for profile updates
 */
@Service
public class UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final ActivityLogService activityLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public UserService(UserRepository userRepository, TenantService tenantService,
                      ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.activityLogService = activityLogService;
    }
    
    @Transactional
    public User createUser(User user, Long tenantId) {
        log.info("Creating user: {} for tenant ID: {}", user.getEmail(), tenantId);
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        Tenant tenant = tenantService.getTenantById(tenantId);
        user.setTenant(tenant);
        
        if (user.getActive() == null) {
            user.setActive(true);
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        User savedUser = userRepository.save(user);
        
        // Log activity
        activityLogService.logActivity(
            tenantId,
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName() + " " + savedUser.getLastName(),
            "User created: " + savedUser.getEmail(),
            "user",
            "New user added to tenant"
        );
        
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return savedUser;
    }
    
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
        
        String oldData = String.format("Name: %s %s, Role: %s, Active: %s", 
            user.getFirstName(), user.getLastName(), user.getRole(), user.getActive());
        
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setRole(userDetails.getRole());
        user.setActive(userDetails.getActive());
        
        User updated = userRepository.save(user);
        
        String newData = String.format("Name: %s %s, Role: %s, Active: %s", 
            updated.getFirstName(), updated.getLastName(), updated.getRole(), updated.getActive());
        
        // Log activity
        activityLogService.logActivity(
            user.getTenant().getId(),
            user.getId(),
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            "User updated: " + user.getEmail(),
            "user",
            String.format("Changed from [%s] to [%s]", oldData, newData)
        );
        
        return updated;
    }
    
    /**
     * FIXED: Added profile update method
     * This allows users to update their own profile without changing role/active status
     */
    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", id);
        
        User user = getUserById(id);
        
        String oldData = String.format("Name: %s %s, Email: %s", 
            user.getFirstName(), user.getLastName(), user.getEmail());
        
        // Update profile fields
        if (request.getFirstName() != null && !request.getFirstName().isEmpty()) {
            user.setFirstName(request.getFirstName());
        }
        
        if (request.getLastName() != null && !request.getLastName().isEmpty()) {
            user.setLastName(request.getLastName());
        }
        
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // Check if new email is already taken by another user
            if (!request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        
        User updated = userRepository.save(user);
        
        String newData = String.format("Name: %s %s, Email: %s", 
            updated.getFirstName(), updated.getLastName(), updated.getEmail());
        
        // Log activity
        activityLogService.logActivity(
            user.getTenant().getId(),
            user.getId(),
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            "Profile updated",
            "user",
            String.format("Changed from [%s] to [%s]", oldData, newData)
        );
        
        log.info("Profile updated successfully for user ID: {}", id);
        
        return updated;
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        
        // Log activity before deletion
        activityLogService.logActivity(
            user.getTenant().getId(),
            user.getId(),
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            "User deleted: " + user.getEmail(),
            "user",
            "User removed from tenant"
        );
        
        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
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
        
        // Log activity
        activityLogService.logActivity(
            user.getTenant().getId(),
            user.getId(),
            user.getEmail(),
            user.getFirstName() + " " + user.getLastName(),
            "Password changed",
            "security",
            "User changed their password"
        );
        
        log.info("Password changed successfully for user ID: {}", userId);
    }
}