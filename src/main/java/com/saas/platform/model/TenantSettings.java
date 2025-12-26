package com.saas.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

//
// TenantSettings - Stores customizable settings per tenant
// Allows white-labeling and configuration
 
@Entity
@Table(name = "tenant_settings")
public class TenantSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;
    
    // Branding
    @Column(name = "primary_color", length = 7)
    private String primaryColor = "#667eea";
    
    @Column(name = "secondary_color", length = 7)
    private String secondaryColor = "#764ba2";
    
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
    
    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;
    
    @Column(name = "company_website", length = 255)
    private String companyWebsite;
    
    // Contact Information
    @Column(name = "support_email", length = 150)
    private String supportEmail;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    // Features Toggles
    @Column(name = "enable_2fa", nullable = false)
    private Boolean enable2FA = false;
    
    @Column(name = "enable_api_access", nullable = false)
    private Boolean enableApiAccess = true;
    
    @Column(name = "enable_webhooks", nullable = false)
    private Boolean enableWebhooks = false;
    
    @Column(name = "enable_sso", nullable = false)
    private Boolean enableSSO = false;
    
    // Email Settings
    @Column(name = "custom_email_domain", length = 100)
    private String customEmailDomain;
    
    @Column(name = "email_from_name", length = 100)
    private String emailFromName;
    
    // Session Settings
    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes = 120;
    
    @Column(name = "max_concurrent_sessions")
    private Integer maxConcurrentSessions = 5;
    
    // Data Retention
    @Column(name = "data_retention_days")
    private Integer dataRetentionDays = 365;
    
    @Column(name = "auto_delete_inactive_users")
    private Boolean autoDeleteInactiveUsers = false;
    
    // Notification Preferences
    @Column(name = "enable_email_notifications", nullable = false)
    private Boolean enableEmailNotifications = true;
    
    @Column(name = "enable_in_app_notifications", nullable = false)
    private Boolean enableInAppNotifications = true;
    
    // Timezone and Locale
    @Column(name = "timezone", length = 50)
    private String timezone = "UTC";
    
    @Column(name = "date_format", length = 20)
    private String dateFormat = "YYYY-MM-DD";
    
    @Column(name = "language", length = 10)
    private String language = "en";
    
    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public TenantSettings() {
    }
    
    public TenantSettings(Tenant tenant) {
        this.tenant = tenant;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    
    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }
    
    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }
    
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    
    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { this.faviconUrl = faviconUrl; }
    
    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
    
    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public Boolean getEnable2FA() { return enable2FA; }
    public void setEnable2FA(Boolean enable2FA) { this.enable2FA = enable2FA; }
    
    public Boolean getEnableApiAccess() { return enableApiAccess; }
    public void setEnableApiAccess(Boolean enableApiAccess) { this.enableApiAccess = enableApiAccess; }
    
    public Boolean getEnableWebhooks() { return enableWebhooks; }
    public void setEnableWebhooks(Boolean enableWebhooks) { this.enableWebhooks = enableWebhooks; }
    
    public Boolean getEnableSSO() { return enableSSO; }
    public void setEnableSSO(Boolean enableSSO) { this.enableSSO = enableSSO; }
    
    public String getCustomEmailDomain() { return customEmailDomain; }
    public void setCustomEmailDomain(String customEmailDomain) { this.customEmailDomain = customEmailDomain; }
    
    public String getEmailFromName() { return emailFromName; }
    public void setEmailFromName(String emailFromName) { this.emailFromName = emailFromName; }
    
    public Integer getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
    
    public Integer getMaxConcurrentSessions() { return maxConcurrentSessions; }
    public void setMaxConcurrentSessions(Integer maxConcurrentSessions) { this.maxConcurrentSessions = maxConcurrentSessions; }
    
    public Integer getDataRetentionDays() { return dataRetentionDays; }
    public void setDataRetentionDays(Integer dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }
    
    public Boolean getAutoDeleteInactiveUsers() { return autoDeleteInactiveUsers; }
    public void setAutoDeleteInactiveUsers(Boolean autoDeleteInactiveUsers) { this.autoDeleteInactiveUsers = autoDeleteInactiveUsers; }
    
    public Boolean getEnableEmailNotifications() { return enableEmailNotifications; }
    public void setEnableEmailNotifications(Boolean enableEmailNotifications) { this.enableEmailNotifications = enableEmailNotifications; }
    
    public Boolean getEnableInAppNotifications() { return enableInAppNotifications; }
    public void setEnableInAppNotifications(Boolean enableInAppNotifications) { this.enableInAppNotifications = enableInAppNotifications; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}