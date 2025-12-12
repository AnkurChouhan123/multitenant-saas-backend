package com.saas.platform.security;

import com.saas.platform.model.User;
import com.saas.platform.model.UserRole;
import com.saas.platform.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * RoleValidator - COMPLETE TENANT_OWNER IMPLEMENTATION
 * This version adds full TENANT_OWNER support without affecting other roles
 */
@Component
public class RoleValidator {
	
	private final UserRepository userRepository;
    
    public RoleValidator(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

    // ========================================
    // CORE AUTHENTICATION METHODS
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
    
    // ========================================
    // TENANT_OWNER SPECIFIC PERMISSIONS
    // ========================================
    
    /**
     * Check if user is TENANT_OWNER
     */
    public boolean isTenantOwner() {
        return getCurrentUser().getRole() == UserRole.TENANT_OWNER;
    }
    
    /**
     * Check if user is TENANT_OWNER of specific tenant
     */
    public boolean isTenantOwnerOf(Long tenantId) {
        User user = getCurrentUser();
        return user.getRole() == UserRole.TENANT_OWNER && 
               user.getTenant().getId().equals(tenantId);
    }
    
    /**
     * Check if user can modify tenant settings
     * TENANT_OWNER and SUPER_ADMIN can modify
     * TENANT_ADMIN cannot modify
     */
    public boolean canModifyTenantSettings(Long tenantId) {
        User user = getCurrentUser();
        
        // SUPER_ADMIN can modify any tenant
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // TENANT_OWNER can modify their own tenant ONLY
        if (user.getRole() == UserRole.TENANT_OWNER && 
            user.getTenant().getId().equals(tenantId)) {
            return true;
        }
        
        // TENANT_ADMIN cannot modify tenant settings
        return false;
    }
    
    /**
     * Require tenant settings permission
     * Only TENANT_OWNER and SUPER_ADMIN
     */
    public void requireTenantSettingsPermission(Long tenantId) {
        User user = getCurrentUser();

        // SUPER_ADMIN can modify any tenant
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }

        // TENANT_OWNER can modify only their own tenant
        if (user.getRole() == UserRole.TENANT_OWNER &&
            user.getTenant().getId().equals(tenantId)) {
            return;
        }

        throw new AccessDeniedException(
            "Only SUPER_ADMIN or TENANT_OWNER can modify tenant settings. " +
            "Current role: " + user.getRole()
        );
    }
    
    // ========================================
    // USER MANAGEMENT PERMISSIONS
    // ========================================
    
    /**
     * Check if user can manage users in tenant
     * TENANT_OWNER, TENANT_ADMIN can manage
     * USER and VIEWER cannot
     */
    public boolean canManageUsers(Long tenantId) {
        User user = getCurrentUser();
        
        // SUPER_ADMIN can manage any tenant's users
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Must belong to the tenant
        if (!user.getTenant().getId().equals(tenantId)) {
            return false;
        }
        
        // TENANT_OWNER and TENANT_ADMIN can manage users
        return user.getRole() == UserRole.TENANT_OWNER || 
               user.getRole() == UserRole.TENANT_ADMIN;
    }
    
    /**
     * Check if user can VIEW users
     * TENANT_OWNER, TENANT_ADMIN, USER can view
     * VIEWER cannot
     */
    public boolean canViewUsers(Long tenantId) {
        User user = getCurrentUser();
        
        // SUPER_ADMIN can view any tenant's users
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Must belong to the tenant
        if (!user.getTenant().getId().equals(tenantId)) {
            return false;
        }
        
        // TENANT_OWNER, TENANT_ADMIN, and USER can view
        return user.getRole() == UserRole.TENANT_OWNER || 
               user.getRole() == UserRole.TENANT_ADMIN ||
               user.getRole() == UserRole.USER;
    }
    
    /**
     * Check if user can modify a specific target user
     * Hierarchy:
     * - SUPER_ADMIN: can modify anyone
     * - TENANT_OWNER: can modify anyone in their tenant (including TENANT_ADMIN)
     * - TENANT_ADMIN: can modify USER and VIEWER only
     * - USER: can only modify themselves
     * - VIEWER: can only modify themselves
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
        
        // TENANT_OWNER can modify anyone in their tenant (except other TENANT_OWNERs)
        if (currentUser.getRole() == UserRole.TENANT_OWNER) {
            // Cannot modify another TENANT_OWNER
            if (targetUser.getRole() == UserRole.TENANT_OWNER) {
                return false;
            }
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
     * Check if user can assign a specific role
     * Rules:
     * - SUPER_ADMIN: can assign any role
     * - TENANT_OWNER: can assign TENANT_ADMIN, USER, VIEWER (NOT TENANT_OWNER or SUPER_ADMIN)
     * - TENANT_ADMIN: can only assign USER and VIEWER
     * - USER/VIEWER: cannot assign roles
     */
    public boolean canAssignRole(UserRole targetRole) {
        User currentUser = getCurrentUser();
        
        // SUPER_ADMIN can assign any role
        if (currentUser.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // TENANT_OWNER can assign TENANT_ADMIN, USER, VIEWER
        // CANNOT assign TENANT_OWNER or SUPER_ADMIN
        if (currentUser.getRole() == UserRole.TENANT_OWNER) {
            return targetRole == UserRole.TENANT_ADMIN ||
                   targetRole == UserRole.USER ||
                   targetRole == UserRole.VIEWER;
        }
        
        // TENANT_ADMIN can only assign USER and VIEWER
        if (currentUser.getRole() == UserRole.TENANT_ADMIN) {
            return targetRole == UserRole.USER || 
                   targetRole == UserRole.VIEWER;
        }
        
        return false;
    }
    
    /**
     * Require permission to modify target user
     */
    public void requireUserModificationPermission(User targetUser) {
        if (!canModifyUser(targetUser)) {
            throw new AccessDeniedException(
                "Insufficient permissions to modify this user. " +
                "Target role: " + targetUser.getRole() + ", " +
                "Your role: " + getCurrentUser().getRole()
            );
        }
    }
    
    /**
     * Require permission to assign role
     */
    public void requireRoleAssignmentPermission(UserRole targetRole) {
        if (!canAssignRole(targetRole)) {
            throw new AccessDeniedException(
                "Insufficient permissions to assign role: " + targetRole + ". " +
                "Your role: " + getCurrentUser().getRole()
            );
        }
    }
    
    /**
     * Require user management permission
     */
    public void requireUserManagementPermission(Long tenantId) {
        if (!canManageUsers(tenantId)) {
            throw new AccessDeniedException(
                "Only TENANT_OWNER, TENANT_ADMIN, or SUPER_ADMIN can manage users"
            );
        }
    }
    
    /**
     * Require user view permission
     */
    public void requireUserViewPermission(Long tenantId) {
        if (!canViewUsers(tenantId)) {
            throw new AccessDeniedException(
                "Access denied: Only TENANT_OWNER, TENANT_ADMIN, and USER can view user list. " +
                "Viewers cannot access user management."
            );
        }
    }
    
    // ========================================
    // SUBSCRIPTION MANAGEMENT
    // ========================================
    
    /**
     * Check if user can VIEW subscription details
     * TENANT_OWNER, TENANT_ADMIN, SUPER_ADMIN can view
     */
    public boolean canViewSubscription(Long tenantId) {
        User user = getCurrentUser();
        
        // SUPER_ADMIN can view any tenant's subscription
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            return true;
        }
        
        // Must belong to the tenant
        if (!user.getTenant().getId().equals(tenantId)) {
            return false;
        }
        
        // TENANT_OWNER and TENANT_ADMIN can view
        return user.getRole() == UserRole.TENANT_OWNER || 
               user.getRole() == UserRole.TENANT_ADMIN;
    }

    /**
     * Check if user can MANAGE subscriptions (upgrade/downgrade/cancel)
     * ONLY TENANT_OWNER can manage
     */
    public boolean canManageSubscription(Long tenantId) {
        User user = getCurrentUser();
        
        // Only TENANT_OWNER can manage their own tenant's subscription
        return user.getRole() == UserRole.TENANT_OWNER && 
               user.getTenant().getId().equals(tenantId);
    }

    /**
     * Require VIEW subscription permission
     */
    public void requireSubscriptionViewPermission(Long tenantId) {
        if (!canViewSubscription(tenantId)) {
            throw new AccessDeniedException(
                "Access denied: Only TENANT_OWNER, TENANT_ADMIN, or SUPER_ADMIN can view subscription details"
            );
        }
    }

    /**
     * Require MANAGE subscription permission
     */
    public void requireSubscriptionPermission(Long tenantId) {
        if (!canManageSubscription(tenantId)) {
            throw new AccessDeniedException(
                "Access denied: Only TENANT_OWNER can manage subscriptions"
            );
        }
    }

    /**
     * Check if has subscription management permission (for UI)
     */
    public boolean hasSubscriptionManagementPermission() {
        try {
            return getCurrentUser().getRole() == UserRole.TENANT_OWNER;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if has subscription view permission (for UI)
     */
    public boolean hasSubscriptionViewPermission() {
        try {
            User currentUser = getCurrentUser();
            UserRole role = currentUser.getRole();
            return role == UserRole.TENANT_OWNER || 
                   role == UserRole.TENANT_ADMIN || 
                   role == UserRole.SUPER_ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ========================================
    // FILE MANAGEMENT PERMISSIONS
    // ========================================
    
    /**
     * Check if user can upload files
     * All except VIEWER can upload
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
     * Check if user can permanently delete files
     * Only SUPER_ADMIN and TENANT_OWNER
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
    
    // ========================================
    // API KEY MANAGEMENT
    // ========================================
    
    /**
     * Check if user can create API keys
     * TENANT_OWNER and TENANT_ADMIN can create
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
                "Only TENANT_OWNER and TENANT_ADMIN can create API keys"
            );
        }
    }
    
    // ========================================
    // WEBHOOK MANAGEMENT
    // ========================================
    
    /**
     * Check if user can manage webhooks
     * TENANT_OWNER and TENANT_ADMIN only
     */
    public boolean canManageWebhooks(Long tenantId) {
        User user = getCurrentUser();
        
        if (user.getTenant().getId().equals(tenantId) &&
            (user.getRole() == UserRole.TENANT_OWNER || 
             user.getRole() == UserRole.TENANT_ADMIN)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Require webhook management permission
     */
    public void requireWebhookPermission(Long tenantId) {
        if (!canManageWebhooks(tenantId)) {
            throw new AccessDeniedException(
                "Only TENANT_OWNER or TENANT_ADMIN can manage webhooks"
            );
        }
    }
    
    // ========================================
    // ANALYTICS & LOGS
    // ========================================
    
    /**
     * Check if user can view detailed logs
     * TENANT_OWNER and TENANT_ADMIN can view
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
     * Require detailed log permission
     */
    public void requireDetailedLogPermission(Long tenantId) {
        if (!canViewDetailedLogs(tenantId)) {
            throw new AccessDeniedException(
                "Access denied: Only TENANT_OWNER and TENANT_ADMIN can view analytics and activity logs"
            );
        }
    }
    
    // ========================================
    // UTILITY METHODS
    // ========================================
    
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
     * Check if user can view tenant
     */
    public boolean canViewTenant(Long tenantId) {
        User user = getCurrentUser();
        return user.getRole() == UserRole.SUPER_ADMIN || 
               user.getTenant().getId().equals(tenantId);
    }
    
    /**
     * Require tenant access
     */
    public void requireTenantAccess(Long tenantId) {
        if (!canViewTenant(tenantId)) {
            throw new AccessDeniedException("Access denied to this tenant");
        }
    }
    
    /**
     * Get current user's tenant ID
     */
    public Long getCurrentTenantId() {
        return getCurrentUser().getTenant().getId();
    }
    
    /**
     * Validate tenant isolation
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
    
    /**
     * Require role
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
}