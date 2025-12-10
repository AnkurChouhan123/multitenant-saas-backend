package com.saas.platform.controller;

import com.saas.platform.model.TenantSettings;
import com.saas.platform.security.RoleValidator;
import com.saas.platform.service.TenantSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * TenantSettingsController with proper permissions:
 * - VIEW: TENANT_OWNER, TENANT_ADMIN, USER, VIEWER (anyone in tenant)
 * - UPDATE: TENANT_OWNER ONLY
 */
@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class TenantSettingsController {
    
    private final TenantSettingsService settingsService;
    private final RoleValidator roleValidator;
    
    public TenantSettingsController(TenantSettingsService settingsService,
                                   RoleValidator roleValidator) {
        this.settingsService = settingsService;
        this.roleValidator = roleValidator;
    }
    
    /**
     * Get settings - Anyone in tenant can view
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<?> getSettings(@PathVariable Long tenantId) {
        try {
            // Verify user belongs to tenant
            roleValidator.requireTenantAccess(tenantId);
            
            TenantSettings settings = settingsService.getSettingsByTenantId(tenantId);
            return ResponseEntity.ok(settings);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update settings - TENANT_OWNER ONLY
     */
    @PutMapping("/tenant/{tenantId}")
    public ResponseEntity<?> updateSettings(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail,
            @RequestBody TenantSettings settings) {
        
        try {
            // Only TENANT_OWNER can update settings
            roleValidator.requireTenantSettingsPermission(tenantId);
            
            TenantSettings updated = settingsService.updateSettings(
                tenantId, settings, userId, userEmail);
            
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Update branding - TENANT_OWNER ONLY
     */
    @PutMapping("/tenant/{tenantId}/branding")
    public ResponseEntity<?> updateBranding(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail,
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String secondaryColor,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(required = false) String faviconUrl) {
        
        try {
            // Only TENANT_OWNER can update branding
            roleValidator.requireTenantSettingsPermission(tenantId);
            
            TenantSettings updated = settingsService.updateBranding(
                tenantId, primaryColor, secondaryColor, logoUrl, faviconUrl, userId, userEmail);
            
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Reset settings - TENANT_OWNER ONLY
     */
    @PostMapping("/tenant/{tenantId}/reset")
    public ResponseEntity<?> resetSettings(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail) {
        
        try {
            // Only TENANT_OWNER can reset settings
            roleValidator.requireTenantSettingsPermission(tenantId);
            
            TenantSettings defaults = settingsService.resetToDefaults(tenantId, userId, userEmail);
            return ResponseEntity.ok(defaults);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check if feature is enabled - Anyone in tenant can view
     */
    @GetMapping("/tenant/{tenantId}/feature/{feature}")
    public ResponseEntity<?> isFeatureEnabled(
            @PathVariable Long tenantId,
            @PathVariable String feature) {
        
        try {
            // Verify user belongs to tenant
            roleValidator.requireTenantAccess(tenantId);
            
            boolean enabled = settingsService.isFeatureEnabled(tenantId, feature);
            return ResponseEntity.ok(enabled);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check if current user can manage tenant settings
     */
    @GetMapping("/tenant/{tenantId}/check-permission")
    public ResponseEntity<Map<String, Object>> checkPermission(@PathVariable Long tenantId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("canView", roleValidator.canViewTenant(tenantId));
            response.put("canManage", roleValidator.canModifyTenantSettings(tenantId));
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