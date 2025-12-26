package com.saas.platform.controller;

import com.saas.platform.dto.PasswordChangeRequest;
import com.saas.platform.dto.UpdateProfileRequest;
import com.saas.platform.model.User;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//
// UserController with proper permissions:
// - TENANT_OWNER, TENANT_ADMIN: Full management (view, create, edit, delete)
// - USER: View only (can see user list, no modifications)
// - VIEWER: No access to user management
 
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserController {
    
    private final UserService userService;
    private final RoleValidator roleValidator;
    
    public UserController(UserService userService, RoleValidator roleValidator) {
        this.userService = userService;
        this.roleValidator = roleValidator;
    }
    
    //
// Get all users in tenant
// VIEW ACCESS: TENANT_OWNER, TENANT_ADMIN, USER (read-only)
// NO ACCESS: VIEWER
     
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<?> getUsersByTenant(@PathVariable Long tenantId) {
        try {
            // Check if user can view users in this tenant
            roleValidator.requireUserViewPermission(tenantId);
            
            List<User> users = userService.getUsersByTenant(tenantId);
            return ResponseEntity.ok(users);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Get user by ID - Anyone in tenant can view
     
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            
            // Verify tenant access
            roleValidator.requireTenantAccess(user.getTenant().getId());
            
            return ResponseEntity.ok(user);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Get user by email - Anyone in tenant can view
     
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            User user = userService.getUserByEmail(email);
            
            // Verify tenant access
            roleValidator.requireTenantAccess(user.getTenant().getId());
            
            return ResponseEntity.ok(user);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Create new user
// MANAGEMENT ACCESS: TENANT_OWNER, TENANT_ADMIN only
     
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, @RequestParam Long tenantId) {
        try {
            // Verify user management permission (TENANT_OWNER, TENANT_ADMIN)
            roleValidator.requireUserManagementPermission(tenantId);
            
            // Verify role assignment permission
            roleValidator.requireRoleAssignmentPermission(user.getRole());
            
            User createdUser = userService.createUser(user, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Change password - Users can change their own password
     
    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @RequestBody PasswordChangeRequest request) {
        try {
            // Users can only change their own password
            User currentUser = roleValidator.getCurrentUser();
            if (!currentUser.getId().equals(id) && !roleValidator.isAdmin()) {
                throw new SecurityException("You can only change your own password");
            }
            
            userService.changePassword(id, request);
            return ResponseEntity.ok("Password changed successfully");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Update user
// MANAGEMENT ACCESS: TENANT_OWNER, TENANT_ADMIN only
     
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            // Get target user
            User targetUser = userService.getUserById(id);
            
            // Verify modification permission (checks role hierarchy)
            roleValidator.requireUserModificationPermission(targetUser);
            
            // If role is changing, verify role assignment permission
            if (!targetUser.getRole().equals(user.getRole())) {
                roleValidator.requireRoleAssignmentPermission(user.getRole());
            }
            
            User updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Update own profile - Everyone can update their own profile
     
    @PatchMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        try {
            // Users can only update their own profile
            User currentUser = roleValidator.getCurrentUser();
            if (!currentUser.getId().equals(id) && !roleValidator.isAdmin()) {
                throw new SecurityException("You can only update your own profile");
            }
            
            User updatedUser = userService.updateProfile(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Delete user
// MANAGEMENT ACCESS: TENANT_OWNER, TENANT_ADMIN only
     
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            // Get target user
            User targetUser = userService.getUserById(id);
            
            // Verify modification permission (checks role hierarchy)
            roleValidator.requireUserModificationPermission(targetUser);
            
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    //
// Check current user's permissions for user management
     
    @GetMapping("/check-permission/{tenantId}")
    public ResponseEntity<Map<String, Object>> checkPermission(@PathVariable Long tenantId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("canView", roleValidator.canViewUsers(tenantId));
            response.put("canManage", roleValidator.canManageUsers(tenantId));
            response.put("role", roleValidator.getCurrentUser().getRole().toString());
        } catch (Exception e) {
            response.put("canView", false);
            response.put("canManage", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    // Helper method
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}