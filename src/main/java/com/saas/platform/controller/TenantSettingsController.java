package com.saas.platform.controller;

import com.saas.platform.model.TenantSettings;
import com.saas.platform.service.TenantSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * TenantSettingsController - REST API for tenant customization
 * Allows tenants to configure branding, features, and preferences
 */
@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class TenantSettingsController {
    
    private final TenantSettingsService settingsService;
    
    public TenantSettingsController(TenantSettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    /**
     * GET /api/settings/tenant/{tenantId} - Get tenant settings
     */
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<TenantSettings> getSettings(@PathVariable Long tenantId) {
        TenantSettings settings = settingsService.getSettingsByTenantId(tenantId);
        return ResponseEntity.ok(settings);
    }
    
    /**
     * PUT /api/settings/tenant/{tenantId} - Update tenant settings
     */
    @PutMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TenantSettings> updateSettings(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail,
            @RequestBody TenantSettings settings) {
        
        TenantSettings updated = settingsService.updateSettings(
            tenantId, settings, userId, userEmail);
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * PUT /api/settings/tenant/{tenantId}/branding - Update branding only
     */
    @PutMapping("/tenant/{tenantId}/branding")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TenantSettings> updateBranding(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail,
            @RequestParam(required = false) String primaryColor,
            @RequestParam(required = false) String secondaryColor,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(required = false) String faviconUrl) {
        
        TenantSettings updated = settingsService.updateBranding(
            tenantId, primaryColor, secondaryColor, logoUrl, faviconUrl, userId, userEmail);
        
        return ResponseEntity.ok(updated);
    }
    
    /**
     * POST /api/settings/tenant/{tenantId}/reset - Reset to defaults
     */
    @PostMapping("/tenant/{tenantId}/reset")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TenantSettings> resetSettings(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail) {
        
        TenantSettings defaults = settingsService.resetToDefaults(tenantId, userId, userEmail);
        return ResponseEntity.ok(defaults);
    }
    
    /**
     * GET /api/settings/tenant/{tenantId}/feature/{feature} - Check if feature enabled
     */
    @GetMapping("/tenant/{tenantId}/feature/{feature}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER')")
    public ResponseEntity<Boolean> isFeatureEnabled(
            @PathVariable Long tenantId,
            @PathVariable String feature) {
        
        boolean enabled = settingsService.isFeatureEnabled(tenantId, feature);
        return ResponseEntity.ok(enabled);
    }
}