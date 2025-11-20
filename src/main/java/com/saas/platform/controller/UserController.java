package com.saas.platform.controller;

import com.saas.platform.dto.PasswordChangeRequest;
import com.saas.platform.dto.UpdateProfileRequest;
import com.saas.platform.model.User;
import com.saas.platform.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController - FIXED
 * Fixed issues:
 * 1. Added profile update endpoint
 * 2. Added proper authorization checks
 * 3. Added PATCH method for profile updates
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<User>> getUsersByTenant(@PathVariable Long tenantId) {
        List<User> users = userService.getUsersByTenant(tenantId);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody User user, @RequestParam Long tenantId) {
        User createdUser = userService.createUser(user, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok("Password changed successfully");
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * FIXED: Added profile update endpoint
     */
    @PatchMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<User> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        User updatedUser = userService.updateProfile(id, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}