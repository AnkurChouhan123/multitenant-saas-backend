package com.saas.platform.model;

/**
 * User Roles Enum - Hierarchical access control
 * 
 * ROLE HIERARCHY:
 * 1. SUPER_ADMIN - Platform-wide operations only (manages all tenants)
 * 2. TENANT_OWNER - Full control within tenant (billing, settings, users, etc.)
 * 3. TENANT_ADMIN - Admin access except billing & tenant settings
 * 4. USER - Basic access, read-only for user lists
 * 5. VIEWER - Minimal access, no user management visibility
 */
public enum UserRole {
    SUPER_ADMIN,    // Platform super admin (across all tenants)
    TENANT_OWNER,   // Owner of tenant - FULL control over tenant
    TENANT_ADMIN,   // Admin of tenant - Admin access EXCEPT billing & settings
    USER,           // Regular user - Basic access, read-only lists
    VIEWER;         // Read-only access - No user management visibility
    
    /**
     * Check if this role has admin privileges
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == TENANT_OWNER || this == TENANT_ADMIN;
    }
    
    /**
     * Check if this role can modify tenant settings
     * ONLY TENANT_OWNER and SUPER_ADMIN
     */
    public boolean canModifyTenantSettings() {
        return this == SUPER_ADMIN || this == TENANT_OWNER;
    }
    
    /**
     * Check if this role can manage users
     * TENANT_OWNER and TENANT_ADMIN can manage users
     */
    public boolean canManageUsers() {
        return this == SUPER_ADMIN || this == TENANT_OWNER || this == TENANT_ADMIN;
    }
    
    /**
     * Check if this role can manage billing/subscriptions
     * ONLY TENANT_OWNER can manage billing
     */
    public boolean canManageBilling() {
        return this == TENANT_OWNER;
    }
    
    /**
     * Check if this role can view analytics and logs
     * TENANT_OWNER and TENANT_ADMIN can view analytics
     */
    public boolean canViewAnalytics() {
        return this == SUPER_ADMIN || this == TENANT_OWNER || this == TENANT_ADMIN;
    }
    
    /**
     * Check if this role can manage webhooks
     * TENANT_OWNER and TENANT_ADMIN can manage webhooks
     */
    public boolean canManageWebhooks() {
        return this == TENANT_OWNER || this == TENANT_ADMIN;
    }
    
    /**
     * Check if this role can view user lists
     * TENANT_OWNER, TENANT_ADMIN, and USER can view
     */
    public boolean canViewUsers() {
        return this == SUPER_ADMIN || this == TENANT_OWNER || 
               this == TENANT_ADMIN || this == USER;
    }
}