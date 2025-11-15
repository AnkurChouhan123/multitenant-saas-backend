package com.saas.platform.model;

/**
 * User Roles Enum
 */
public enum UserRole {
    SUPER_ADMIN,   // Platform super admin (across all tenants)
    TENANT_ADMIN,  // Admin of a specific tenant
    USER,          // Regular user
    VIEWER         // Read-only access
}