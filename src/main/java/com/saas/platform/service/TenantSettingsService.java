package com.saas.platform.service;

import com.saas.platform.model.Tenant;
import com.saas.platform.model.TenantSettings;
import com.saas.platform.repository.TenantSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//
// TenantSettingsService - Business logic for tenant customization
// Handles white-labeling and configuration options per tenant
 
@Service
public class TenantSettingsService {
    
    private static final Logger log = LoggerFactory.getLogger(TenantSettingsService.class);
    
    private final TenantSettingsRepository settingsRepository;
    private final TenantService tenantService;
    private final ActivityLogService activityLogService;
    
    public TenantSettingsService(TenantSettingsRepository settingsRepository,
                                TenantService tenantService,
                                ActivityLogService activityLogService) {
        this.settingsRepository = settingsRepository;
        this.tenantService = tenantService;
        this.activityLogService = activityLogService;
    }
    
    //
// Get settings for a tenant (create default if not exists)
     
    public TenantSettings getSettingsByTenantId(Long tenantId) {
        return settingsRepository.findByTenantId(tenantId)
            .orElseGet(() -> createDefaultSettings(tenantId));
    }
    
    //
// Create default settings for new tenant
     
    @Transactional
    public TenantSettings createDefaultSettings(Long tenantId) {
        log.info("Creating default settings for tenant ID: {}", tenantId);
        
        Tenant tenant = tenantService.getTenantById(tenantId);
        
        TenantSettings settings = new TenantSettings(tenant);
        // Default values are set in the entity
        
        TenantSettings saved = settingsRepository.save(settings);
        log.info("Default settings created for tenant ID: {}", tenantId);
        
        return saved;
    }
    
    //
// Update tenant settings
     
    @Transactional
    public TenantSettings updateSettings(Long tenantId, TenantSettings updatedSettings, 
                                        Long userId, String userEmail) {
        log.info("Updating settings for tenant ID: {}", tenantId);
        
        TenantSettings existing = getSettingsByTenantId(tenantId);
        
        // Update branding
        if (updatedSettings.getPrimaryColor() != null) {
            existing.setPrimaryColor(updatedSettings.getPrimaryColor());
        }
        if (updatedSettings.getSecondaryColor() != null) {
            existing.setSecondaryColor(updatedSettings.getSecondaryColor());
        }
        if (updatedSettings.getLogoUrl() != null) {
            existing.setLogoUrl(updatedSettings.getLogoUrl());
        }
        if (updatedSettings.getFaviconUrl() != null) {
            existing.setFaviconUrl(updatedSettings.getFaviconUrl());
        }
        if (updatedSettings.getCompanyWebsite() != null) {
            existing.setCompanyWebsite(updatedSettings.getCompanyWebsite());
        }
        
        // Update contact info
        if (updatedSettings.getSupportEmail() != null) {
            existing.setSupportEmail(updatedSettings.getSupportEmail());
        }
        if (updatedSettings.getPhoneNumber() != null) {
            existing.setPhoneNumber(updatedSettings.getPhoneNumber());
        }
        if (updatedSettings.getAddress() != null) {
            existing.setAddress(updatedSettings.getAddress());
        }
        
        // Update features
        if (updatedSettings.getEnable2FA() != null) {
            existing.setEnable2FA(updatedSettings.getEnable2FA());
        }
        if (updatedSettings.getEnableApiAccess() != null) {
            existing.setEnableApiAccess(updatedSettings.getEnableApiAccess());
        }
        if (updatedSettings.getEnableWebhooks() != null) {
            existing.setEnableWebhooks(updatedSettings.getEnableWebhooks());
        }
        if (updatedSettings.getEnableSSO() != null) {
            existing.setEnableSSO(updatedSettings.getEnableSSO());
        }
        
        // Update email settings
        if (updatedSettings.getCustomEmailDomain() != null) {
            existing.setCustomEmailDomain(updatedSettings.getCustomEmailDomain());
        }
        if (updatedSettings.getEmailFromName() != null) {
            existing.setEmailFromName(updatedSettings.getEmailFromName());
        }
        
        // Update session settings
        if (updatedSettings.getSessionTimeoutMinutes() != null) {
            existing.setSessionTimeoutMinutes(updatedSettings.getSessionTimeoutMinutes());
        }
        if (updatedSettings.getMaxConcurrentSessions() != null) {
            existing.setMaxConcurrentSessions(updatedSettings.getMaxConcurrentSessions());
        }
        
        // Update data retention
        if (updatedSettings.getDataRetentionDays() != null) {
            existing.setDataRetentionDays(updatedSettings.getDataRetentionDays());
        }
        if (updatedSettings.getAutoDeleteInactiveUsers() != null) {
            existing.setAutoDeleteInactiveUsers(updatedSettings.getAutoDeleteInactiveUsers());
        }
        
        // Update notifications
        if (updatedSettings.getEnableEmailNotifications() != null) {
            existing.setEnableEmailNotifications(updatedSettings.getEnableEmailNotifications());
        }
        if (updatedSettings.getEnableInAppNotifications() != null) {
            existing.setEnableInAppNotifications(updatedSettings.getEnableInAppNotifications());
        }
        
        // Update locale
        if (updatedSettings.getTimezone() != null) {
            existing.setTimezone(updatedSettings.getTimezone());
        }
        if (updatedSettings.getDateFormat() != null) {
            existing.setDateFormat(updatedSettings.getDateFormat());
        }
        if (updatedSettings.getLanguage() != null) {
            existing.setLanguage(updatedSettings.getLanguage());
        }
        
        TenantSettings saved = settingsRepository.save(existing);
        
        // Log activity
        activityLogService.logActivity(
            tenantId,
            userId,
            userEmail,
            "System",
            "Tenant settings updated",
            "settings",
            "Configuration changes applied"
        );
        
        log.info("Settings updated successfully for tenant ID: {}", tenantId);
        
        return saved;
    }
    
    //
// Update branding only
     
    @Transactional
    public TenantSettings updateBranding(Long tenantId, String primaryColor, 
                                        String secondaryColor, String logoUrl, 
                                        String faviconUrl, Long userId, String userEmail) {
        TenantSettings settings = getSettingsByTenantId(tenantId);
        
        if (primaryColor != null) settings.setPrimaryColor(primaryColor);
        if (secondaryColor != null) settings.setSecondaryColor(secondaryColor);
        if (logoUrl != null) settings.setLogoUrl(logoUrl);
        if (faviconUrl != null) settings.setFaviconUrl(faviconUrl);
        
        TenantSettings saved = settingsRepository.save(settings);
        
        activityLogService.logActivity(
            tenantId, userId, userEmail, "System",
            "Branding updated", "settings",
            "Visual customization applied"
        );
        
        return saved;
    }
    
    //
// Reset settings to defaults
     
    @Transactional
    public TenantSettings resetToDefaults(Long tenantId, Long userId, String userEmail) {
        log.info("Resetting settings to defaults for tenant ID: {}", tenantId);
        
        settingsRepository.deleteByTenantId(tenantId);
        TenantSettings defaults = createDefaultSettings(tenantId);
        
        activityLogService.logActivity(
            tenantId, userId, userEmail, "System",
            "Settings reset to defaults", "settings",
            "All customizations removed"
        );
        
        return defaults;
    }
    
    //
// Check if feature is enabled for tenant
     
    public boolean isFeatureEnabled(Long tenantId, String feature) {
        TenantSettings settings = getSettingsByTenantId(tenantId);
        
        return switch (feature.toLowerCase()) {
            case "2fa" -> settings.getEnable2FA();
            case "api" -> settings.getEnableApiAccess();
            case "webhooks" -> settings.getEnableWebhooks();
            case "sso" -> settings.getEnableSSO();
            default -> false;
        };
    }
}