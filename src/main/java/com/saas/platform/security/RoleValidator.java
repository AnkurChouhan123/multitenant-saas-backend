package com.saas.platform.security;

import com.saas.platform.model.User;
import com.saas.platform.model.UserRole;
import com.saas.platform.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * RoleValidator - UPDATED with all enhanced permission checks
 * This is your existing file with new methods added
 */
@Component
public class RoleValidator {
	
	private final UserRepository userRepository;
    
    public RoleValidator(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}

	// ========================================
    // EXISTING CORE METHODS (Keep as is)
    // ========================================
    
	/**
     * Get current authenticated user
     */
    public User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated()) {
                throw new AccessDeniedException("Not authenticated");
            }
            
            String email = auth.getName();
            
            if (email == null || email.equals("anonymousUser")) {
                throw new AccessDeniedException("Anonymous user not allowed");
            }
            
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new AccessDeniedException("User not found: " + email));
                    
        } catch (Exception e) {
            throw new AccessDeniedException("Authentication failed: " + e.getMessage());
        }
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
    
    // ========================================
    // NEW ENHANCED METHODS - Add these to existing file
    // ========================================
    
    /**
     * Check if user can view tenant (belongs to it or is SUPER_ADMIN)
     */
    public boolean canViewTenant(Long tenantId) {
        User user = getCurrentUser();
        return user.getRole() == UserRole.SUPER_ADMIN || 
               user.getTenant().getId().equals(tenantId);
    }
    
    /**
     * Require tenant view access
     */
    public void requireTenantAccess(Long tenantId) {
        if (!canViewTenant(tenantId)) {
            throw new AccessDeniedException("Access denied to this tenant");
        }
    }
    
    /**
     * Check if user can modify a specific target user
     * Rules:
     * - SUPER_ADMIN can modify anyone
     * - TENANT_OWNER can modify anyone in their tenant
     * - TENANT_ADMIN can modify USER and VIEWER only
     * - USER can only modify themselves
     */
    public boolean canModifyUser(User targetUser) {
        User currentUser = getCurrentUser();
        
        // SUPER_ADMIN can modify anyone
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Can always modify self
        if (currentUser.getId().equals(targetUser.getId())) {
            return true;
        }
        
        // Must be in same tenant
        if (!currentUser.getTenant().getId().equals(targetUser.getTenant().getId())) {
            return false;
        }
        
        // TENANT_OWNER can modify anyone in their tenant
        if (currentUser.getRole() == UserRole.TENANT_OWNER) {
            return true;
        }
        
        // TENANT_ADMIN can only modify USER and VIEWER
        if (currentUser.getRole() == UserRole.TENANT_ADMIN) {
            return targetUser.getRole() == UserRole.USER || 
                   targetUser.getRole() == UserRole.VIEWER;
        }
        
        return false;
    }
    
    /**
     * Require permission to modify target user
     */
    public void requireUserModificationPermission(User targetUser) {
        if (!canModifyUser(targetUser)) {
            throw new AccessDeniedException(
                "Insufficient permissions to modify this user"
            );
        }
    }
    
    /**
     * Check if user can assign a specific role
     * Rules:
     * - SUPER_ADMIN can assign any role
     * - TENANT_OWNER can assign any role except SUPER_ADMIN
     * - TENANT_ADMIN can only assign USER and VIEWER
     */
    public boolean canAssignRole(UserRole targetRole) {
        User currentUser = getCurrentUser();
        
        // SUPER_ADMIN can assign any role
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // TENANT_OWNER can assign anything except SUPER_ADMIN
        if (currentUser.getRole() == UserRole.TENANT_OWNER) {
            return targetRole != UserRole.SUPER_ADMIN;
        }
        
        // TENANT_ADMIN can only assign USER and VIEWER
        if (currentUser.getRole() == UserRole.TENANT_ADMIN) {
            return targetRole == UserRole.USER || targetRole == UserRole.VIEWER;
        }
        
        return false;
    }
    
    /**
     * Require permission to assign role
     */
    public void requireRoleAssignmentPermission(UserRole targetRole) {
        if (!canAssignRole(targetRole)) {
            throw new AccessDeniedException(
                "Insufficient permissions to assign role: " + targetRole
            );
        }
    }
    
    /**
     * Check if user can upload files
     * All authenticated users except VIEWER can upload
     */
    public boolean canUploadFiles() {
        return getCurrentUser().getRole() != UserRole.VIEWER;
    }
    
    /**
     * Require upload permission
     */
    public void requireUploadPermission() {
        if (!canUploadFiles()) {
            throw new AccessDeniedException("Viewers cannot upload files");
        }
    }
    
    /**
     * Check if user can delete file permanently
     * Only SUPER_ADMIN and TENANT_OWNER can permanently delete
     */
    public boolean canPermanentlyDeleteFiles(Long tenantId) {
        User user = getCurrentUser();
        return user.getRole() == UserRole.SUPER_ADMIN ||
               (user.getRole() == UserRole.TENANT_OWNER && 
                user.getTenant().getId().equals(tenantId));
    }
    
    /**
     * Require permanent delete permission
     */
    public void requirePermanentDeletePermission(Long tenantId) {
        if (!canPermanentlyDeleteFiles(tenantId)) {
            throw new AccessDeniedException(
                "Only TENANT_OWNER or SUPER_ADMIN can permanently delete files"
            );
        }
    }
    
    /**
     * Check if user can create API keys
     * SUPER_ADMIN, TENANT_OWNER, TENANT_ADMIN can create
     */
    public boolean canCreateApiKeys(Long tenantId) {
        User user = getCurrentUser();
        
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        if (user.getTenant().getId().equals(tenantId) &&
            (user.getRole() == UserRole.TENANT_OWNER || 
             user.getRole() == UserRole.TENANT_ADMIN)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Require API key creation permission
     */
    public void requireApiKeyCreationPermission(Long tenantId) {
        if (!canCreateApiKeys(tenantId)) {
            throw new AccessDeniedException(
                "Only admins can create API keys"
            );
        }
    }
    
    /**
     * Check if user can manage subscriptions
     * Only SUPER_ADMIN and TENANT_OWNER
     */
    public boolean canManageSubscription(Long tenantId) {
        User user = getCurrentUser();
        return user.getRole() == UserRole.SUPER_ADMIN ||
               (user.getRole() == UserRole.TENANT_OWNER && 
                user.getTenant().getId().equals(tenantId));
    }
    
    /**
     * Require subscription management permission
     */
    public void requireSubscriptionPermission(Long tenantId) {
        if (!canManageSubscription(tenantId)) {
            throw new AccessDeniedException(
                "Only TENANT_OWNER or SUPER_ADMIN can manage subscriptions"
            );
        }
    }
    
    /**
     * Check if user can manage webhooks
     * Only SUPER_ADMIN and TENANT_OWNER
     */
    public boolean canManageWebhooks(Long tenantId) {
        User user = getCurrentUser();
        return user.getRole() == UserRole.SUPER_ADMIN ||
               (user.getRole() == UserRole.TENANT_OWNER && 
                user.getTenant().getId().equals(tenantId));
    }
    
    /**
     * Require webhook management permission
     */
    public void requireWebhookPermission(Long tenantId) {
        if (!canManageWebhooks(tenantId)) {
            throw new AccessDeniedException(
                "Only TENANT_OWNER or SUPER_ADMIN can manage webhooks"
            );
        }
    }
    
    /**
     * Check if user can view detailed logs
     * Only admins can view all activity logs
     */
    public boolean canViewDetailedLogs(Long tenantId) {
        User user = getCurrentUser();
        
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        if (user.getTenant().getId().equals(tenantId) &&
            (user.getRole() == UserRole.TENANT_OWNER || 
             user.getRole() == UserRole.TENANT_ADMIN)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if user is admin (any admin role)
     */
    public boolean isAdmin() {
        UserRole role = getCurrentUser().getRole();
        return role == UserRole.SUPER_ADMIN || 
               role == UserRole.TENANT_OWNER || 
               role == UserRole.TENANT_ADMIN;
    }
    
    /**
     * Check if user is super admin
     */
    public boolean isSuperAdmin() {
        return getCurrentUser().getRole() == UserRole.SUPER_ADMIN;
    }
    
    /**
     * Check if user is tenant owner
     */
    public boolean isTenantOwner() {
        return getCurrentUser().getRole() == UserRole.TENANT_OWNER;
    }
    
    /**
     * Get current user's tenant ID
     */
    public Long getCurrentTenantId() {
        return getCurrentUser().getTenant().getId();
    }
    
    /**
     * Validate tenant isolation
     * Throws exception if user tries to access data from another tenant
     */
    public void validateTenantIsolation(Long resourceTenantId) {
        User currentUser = getCurrentUser();
        
        // SUPER_ADMIN can access any tenant
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        
        // Others can only access their own tenant
        if (!currentUser.getTenant().getId().equals(resourceTenantId)) {
            throw new AccessDeniedException(
                "Cannot access resources from another tenant"
            );
        }
    }
}