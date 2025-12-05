
package com.saas.platform.security;

import com.saas.platform.model.User;
import com.saas.platform.model.UserRole;
import com.saas.platform.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RoleValidator {
	
	UserRepository userRepository;
	
    
    public RoleValidator(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}

	/**
     * Get current authenticated user
     */
	public User getCurrentUser() {
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    
	    String email = auth.getName();  // always safe
	    
	    return userRepository.findByEmail(email)
	            .orElseThrow(() -> new RuntimeException("User not found: " + email));
	}

    /**
     * Check if current user has required role
     */
    public boolean hasRole(UserRole... roles) {
        User user = getCurrentUser();
        for (UserRole role : roles) {
            if (user.getRole() == role) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if current user can modify tenant settings
     */
    public boolean canModifyTenantSettings(Long tenantId) {
        User user = getCurrentUser();
        
        // Super admin can modify any tenant
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Tenant owner can modify their own tenant
        if (user.getRole() == UserRole.TENANT_OWNER && 
            user.getTenant().getId().equals(tenantId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if current user can manage users in tenant
     */
    public boolean canManageUsers(Long tenantId) {
        User user = getCurrentUser();
        
        // Super admin can manage any tenant's users
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Tenant owner/admin can manage their own tenant's users
        if ((user.getRole() == UserRole.TENANT_OWNER || 
             user.getRole() == UserRole.TENANT_ADMIN) && 
            user.getTenant().getId().equals(tenantId)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Require role or throw exception
     */
    public void requireRole(UserRole... roles) {
        if (!hasRole(roles)) {
            User user = getCurrentUser();
            throw new AccessDeniedException(
                String.format("Access denied. Required: %s, Current: %s", 
                    java.util.Arrays.toString(roles), user.getRole())
            );
        }
    }
    
    /**
     * Require tenant settings permission or throw exception
     */
    public void requireTenantSettingsPermission(Long tenantId) {
        User user = getCurrentUser();

        // SUPER_ADMIN → can modify ANY tenant
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        // TENANT_OWNER → can modify ONLY their own tenant
        if (user.getRole() == UserRole.TENANT_OWNER &&
            user.getTenant().getId().equals(tenantId)) {
            return;
        }

        throw new AccessDeniedException(
            "Only SUPER_ADMIN or TENANT_OWNER can modify tenant settings"
        );
    }
    /**
     * Require user management permission or throw exception
     */
    public void requireUserManagementPermission(Long tenantId) {
        if (!canManageUsers(tenantId)) {
            throw new AccessDeniedException(
                "Only TENANT_OWNER, TENANT_ADMIN, or SUPER_ADMIN can manage users"
            );
        }
    }
}