package com.saas.platform.model;

/**
 * User Roles Enum - Hierarchical access control
 */
public enum UserRole {
    SUPER_ADMIN,    // Platform super admin (across all tenants)
    TENANT_OWNER,   // 🆕 Owner of tenant - Full control over tenant
    TENANT_ADMIN,   // Admin of a specific tenant - Can manage users
    USER,           // Regular user - Basic access
    VIEWER;         // Read-only access
    
    /**
     * Check if this role has admin privileges
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == TENANT_OWNER || this == TENANT_ADMIN;
    }
    
    /**
     * Check if this role can modify tenant settings
     */
    public boolean canModifyTenantSettings() {
        return this == SUPER_ADMIN || this == TENANT_OWNER;
    }
    
    /**
     * Check if this role can manage users
     */
    public boolean canManageUsers() {
        return this == SUPER_ADMIN || this == TENANT_OWNER || this == TENANT_ADMIN;
    }
}
