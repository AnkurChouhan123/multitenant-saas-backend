//package com.saas.platform.controller;
//
//import com.saas.platform.model.User;
//import com.saas.platform.security.RoleValidator;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
////
// * PermissionController - API for frontend to check user permissions
// * 
// * This controller provides endpoints for the frontend to determine
// * what UI elements and pages the current user can access.
// 
//@RestController
//@RequestMapping("/api/permissions")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
//public class PermissionController {
//    
//    private final RoleValidator roleValidator;
//    
//    public PermissionController(RoleValidator roleValidator) {
//        this.roleValidator = roleValidator;
//    }
//    
//    //
//     * Get all permissions for current user
//     * Returns comprehensive permission set for UI rendering
//     
//    @GetMapping("/current")
//    public ResponseEntity<Map<String, Object>> getCurrentUserPermissions() {
//        try {
//            User currentUser = roleValidator.getCurrentUser();
//            Long tenantId = currentUser.getTenant().getId();
//            
//            Map<String, Object> permissions = new HashMap<>();
//            
//            // User info
//            permissions.put("userId", currentUser.getId());
//            permissions.put("email", currentUser.getEmail());
//            permissions.put("role", currentUser.getRole().toString());
//            permissions.put("tenantId", tenantId);
//            permissions.put("tenantName", currentUser.getTenant().getName());
//            
//            // Billing & Subscription permissions
//            permissions.put("canManageBilling", roleValidator.canManageBilling(tenantId));
//            permissions.put("canViewSubscription", roleValidator.canViewSubscription(tenantId));
//            
//            // Tenant Settings permissions
//            permissions.put("canModifyTenantSettings", roleValidator.canModifyTenantSettings(tenantId));
//            permissions.put("canViewTenantSettings", roleValidator.canViewTenantSettings(tenantId));
//            
//            // User Management permissions
//            permissions.put("canManageUsers", roleValidator.canManageUsers(tenantId));
//            permissions.put("canViewUsers", roleValidator.canViewUsers(tenantId));
//            
//            // Analytics & Logs permissions
//            permissions.put("canViewAnalytics", roleValidator.canViewAnalytics(tenantId));
//            permissions.put("canViewActivityLogs", roleValidator.canViewDetailedLogs(tenantId));
//            
//            // Webhook permissions
//            permissions.put("canManageWebhooks", roleValidator.canManageWebhooks(tenantId));
//            
//            // API Key permissions
//            permissions.put("canCreateApiKeys", roleValidator.canCreateApiKeys(tenantId));
//            
//            // File operations permissions
//            permissions.put("canUploadFiles", roleValidator.canUploadFiles());
//            permissions.put("canPermanentlyDeleteFiles", roleValidator.canPermanentlyDeleteFiles(tenantId));
//            
//            // Role checks
//            permissions.put("isAdmin", roleValidator.isAdmin());
//            permissions.put("isSuperAdmin", roleValidator.isSuperAdmin());
//            permissions.put("isTenantOwner", roleValidator.isTenantOwner());
//            permissions.put("isTenantAdmin", roleValidator.isTenantAdmin());
//            
//            return ResponseEntity.ok(permissions);
//            
//        } catch (Exception e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            return ResponseEntity.status(401).body(error);
//        }
//    }
//    
//    //
//     * Check specific permission
//     
//    @GetMapping("/check")
//    public ResponseEntity<Map<String, Boolean>> checkPermission(
//            @RequestParam String permission,
//            @RequestParam(required = false) Long tenantId) {
//        
//        Map<String, Boolean> response = new HashMap<>();
//        
//        try {
//            User currentUser = roleValidator.getCurrentUser();
//            Long userTenantId = tenantId != null ? tenantId : currentUser.getTenant().getId();
//            
//            boolean hasPermission = switch (permission.toLowerCase()) {
//                case "manage_billing" -> roleValidator.canManageBilling(userTenantId);
//                case "view_subscription" -> roleValidator.canViewSubscription(userTenantId);
//                case "modify_tenant_settings" -> roleValidator.canModifyTenantSettings(userTenantId);
//                case "view_tenant_settings" -> roleValidator.canViewTenantSettings(userTenantId);
//                case "manage_users" -> roleValidator.canManageUsers(userTenantId);
//                case "view_users" -> roleValidator.canViewUsers(userTenantId);
//                case "view_analytics" -> roleValidator.canViewAnalytics(userTenantId);
//                case "view_logs" -> roleValidator.canViewDetailedLogs(userTenantId);
//                case "manage_webhooks" -> roleValidator.canManageWebhooks(userTenantId);
//                case "create_api_keys" -> roleValidator.canCreateApiKeys(userTenantId);
//                case "upload_files" -> roleValidator.canUploadFiles();
//                case "permanently_delete_files" -> roleValidator.canPermanentlyDeleteFiles(userTenantId);
//                default -> false;
//            };
//            
//            response.put("hasPermission", hasPermission);
//            response.put("role", true); // Include role info
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            response.put("hasPermission", false);
//            response.put("error", true);
//            return ResponseEntity.status(401).body(response);
//        }
//    }
//    
//    //
//     * Check if user can access a specific page
//     
//    @GetMapping("/page-access")
//    public ResponseEntity<Map<String, Object>> checkPageAccess(@RequestParam String page) {
//        Map<String, Object> response = new HashMap<>();
//        
//        try {
//            User currentUser = roleValidator.getCurrentUser();
//            Long tenantId = currentUser.getTenant().getId();
//            
//            boolean canAccess = switch (page.toLowerCase()) {
//                case "billing", "subscription", "payment" -> 
//                    roleValidator.canManageBilling(tenantId);
//                case "settings", "tenant-settings", "branding" -> 
//                    roleValidator.canModifyTenantSettings(tenantId);
//                case "analytics", "dashboard" -> 
//                    roleValidator.canViewAnalytics(tenantId);
//                case "activity-logs", "audit-logs" -> 
//                    roleValidator.canViewDetailedLogs(tenantId);
//                case "users", "user-management" -> 
//                    roleValidator.canViewUsers(tenantId);
//                case "webhooks" -> 
//                    roleValidator.canManageWebhooks(tenantId);
//                case "api-keys" -> 
//                    roleValidator.canCreateApiKeys(tenantId);
//                default -> true; // Allow access to general pages by default
//            };
//            
//            response.put("canAccess", canAccess);
//            response.put("role", currentUser.getRole().toString());
//            
//            if (!canAccess) {
//                response.put("reason", getAccessDeniedReason(page, currentUser.getRole().toString()));
//            }
//            
//            return ResponseEntity.ok(response);
//            
//        } catch (Exception e) {
//            response.put("canAccess", false);
//            response.put("error", e.getMessage());
//            return ResponseEntity.status(401).body(response);
//        }
//    }
//    
//    //
//     * Get UI configuration based on user role
//     * Tells frontend what to show/hide
//     
//    @GetMapping("/ui-config")
//    public ResponseEntity<Map<String, Object>> getUiConfig() {
//        try {
//            User currentUser = roleValidator.getCurrentUser();
//            Long tenantId = currentUser.getTenant().getId();
//            
//            Map<String, Object> config = new HashMap<>();
//            
//            // Navigation menu items
//            Map<String, Boolean> menuItems = new HashMap<>();
//            menuItems.put("dashboard", true);
//            menuItems.put("users", roleValidator.canViewUsers(tenantId));
//            menuItems.put("settings", roleValidator.canViewTenantSettings(tenantId));
//            menuItems.put("billing", roleValidator.canViewSubscription(tenantId));
//            menuItems.put("analytics", roleValidator.canViewAnalytics(tenantId));
//            menuItems.put("activityLogs", roleValidator.canViewDetailedLogs(tenantId));
//            menuItems.put("webhooks", roleValidator.canManageWebhooks(tenantId));
//            menuItems.put("apiKeys", roleValidator.canCreateApiKeys(tenantId));
//            
//            config.put("menuItems", menuItems);
//            
//            // Action buttons visibility
//            Map<String, Boolean> actions = new HashMap<>();
//            actions.put("createUser", roleValidator.canManageUsers(tenantId));
//            actions.put("editUser", roleValidator.canManageUsers(tenantId));
//            actions.put("deleteUser", roleValidator.canManageUsers(tenantId));
//            actions.put("modifySettings", roleValidator.canModifyTenantSettings(tenantId));
//            actions.put("changePlan", roleValidator.canManageBilling(tenantId));
//            actions.put("updatePayment", roleValidator.canManageBilling(tenantId));
//            actions.put("cancelSubscription", roleValidator.canManageBilling(tenantId));
//            actions.put("uploadFiles", roleValidator.canUploadFiles());
//            
//            config.put("actions", actions);
//            
//            // Feature flags
//            Map<String, Boolean> features = new HashMap<>();
//            features.put("billingManagement", roleValidator.canManageBilling(tenantId));
//            features.put("tenantSettingsEdit", roleValidator.canModifyTenantSettings(tenantId));
//            features.put("userManagement", roleValidator.canManageUsers(tenantId));
//            features.put("analyticsAccess", roleValidator.canViewAnalytics(tenantId));
//            
//            config.put("features", features);
//            
//            return ResponseEntity.ok(config);
//            
//        } catch (Exception e) {
//            Map<String, Object> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            return ResponseEntity.status(401).body(error);
//        }
//    }
//    
//    // Helper method
//    private String getAccessDeniedReason(String page, String role) {
//        return switch (page.toLowerCase()) {
//            case "billing", "subscription", "payment" ->
//                "Only TENANT_OWNER can access billing and subscription management. " +
//                "Your role: " + role;
//            case "settings", "tenant-settings", "branding" ->
//                "Only TENANT_OWNER can modify tenant settings. " +
//                "TENANT_ADMIN can view but not modify. Your role: " + role;
//            case "analytics", "dashboard", "activity-logs" ->
//                "Only TENANT_OWNER and TENANT_ADMIN can access analytics and logs. " +
//                "Your role: " + role;
//            case "users", "user-management" ->
//                "VIEWER role cannot access user management. " +
//                "Your role: " + role;
//            default ->
//                "Insufficient permissions to access this page. Your role: " + role;
//        };
//    }
//}