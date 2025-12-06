
package com.saas.platform.controller;

import com.saas.platform.model.TenantSettings;
import com.saas.platform.service.TenantSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:3000")
public class TenantSettingsController {
    
    private final TenantSettingsService settingsService;
    
    public TenantSettingsController(TenantSettingsService settingsService) {
        this.settingsService = settingsService;
    }
    
    // view settings
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<TenantSettings> getSettings(@PathVariable Long tenantId) {
        TenantSettings settings = settingsService.getSettingsByTenantId(tenantId);
        return ResponseEntity.ok(settings);
    }
    
    // update settings
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
    
//    update branding
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
    
    // reset settings
    @PostMapping("/tenant/{tenantId}/reset")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<TenantSettings> resetSettings(
            @PathVariable Long tenantId,
            @RequestParam Long userId,
            @RequestParam String userEmail) {
        
        TenantSettings defaults = settingsService.resetToDefaults(tenantId, userId, userEmail);
        return ResponseEntity.ok(defaults);
    }
    
    // check features
    @GetMapping("/tenant/{tenantId}/feature/{feature}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPER_ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<Boolean> isFeatureEnabled(
            @PathVariable Long tenantId,
            @PathVariable String feature) {
        
        boolean enabled = settingsService.isFeatureEnabled(tenantId, feature);
        return ResponseEntity.ok(enabled);
    }
}