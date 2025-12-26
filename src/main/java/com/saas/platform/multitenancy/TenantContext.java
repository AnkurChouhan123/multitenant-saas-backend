package com.saas.platform.multitenancy;

//
// TenantContext - Thread-safe storage for current tenant
// Uses ThreadLocal to ensure tenant isolation per request
 
public class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    
    //
// Set the current tenant ID for this thread/request
     
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    //
// Get the current tenant ID
     
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    //
// Clear the tenant context (called after request completes)
     
    public static void clear() {
        CURRENT_TENANT.remove();
    }
    
    //
// Check if tenant context is set
     
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }
}