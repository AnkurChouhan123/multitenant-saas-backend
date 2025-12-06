package com.saas.platform.controller;

import com.saas.platform.dto.PasswordChangeRequest;
import com.saas.platform.dto.UpdateProfileRequest;
import com.saas.platform.model.User;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    
    private final UserService userService;
    private final RoleValidator roleValidator;
    
    public UserController(UserService userService, RoleValidator roleValidator) {
        this.userService = userService;
        this.roleValidator = roleValidator;
    }
    
    // Get all users in tenant
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<User>> getUsersByTenant(@PathVariable Long tenantId) {
        // Verify user management permission
        roleValidator.requireUserManagementPermission(tenantId);
        
        List<User> users = userService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(users);
    }
    
    // Get user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    //Get user by email
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    // Create new user
    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody User user, @RequestParam Long tenantId) {
        // Verify user management permission
        roleValidator.requireUserManagementPermission(tenantId);
        
        // Verify role assignment permission
        roleValidator.requireRoleAssignmentPermission(user.getRole());
        
        User createdUser = userService.createUser(user, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    //change password
     
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok("Password changed successfully");
    }
    
    // Update user
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
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
    }
    
    // Update own profile 
    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<User> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateProfile(id, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    // Delete user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // Get target user
        User targetUser = userService.getUserById(id);
        
        // Verify modification permission (checks role hierarchy)
        roleValidator.requireUserModificationPermission(targetUser);
        
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}