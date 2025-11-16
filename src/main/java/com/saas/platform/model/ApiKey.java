package com.saas.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API Key Entity - For tenant API access management
 */
@Entity
@Table(name = "api_keys")
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 64)
    private String keyValue;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "usage_count")
    private Long usageCount = 0L;
    
    @Column(name = "rate_limit_per_hour")
    private Integer rateLimitPerHour = 1000;
    
    @Column(name = "allowed_ips", length = 500)
    private String allowedIps; // Comma-separated IPs
    
    @Column(name = "scopes", length = 500)
    private String scopes; // read, write, admin
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ApiKey() {
    }
    
    public ApiKey(String name, Long tenantId, Long createdBy) {
        this.name = name;
        this.tenantId = tenantId;
        this.createdBy = createdBy;
        this.keyValue = generateApiKey();
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }
    
    // Generate secure API key
    private String generateApiKey() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getKeyValue() {
        return keyValue;
    }
    
    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
    
    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
    
    public Long getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Long usageCount) {
        this.usageCount = usageCount;
    }
    
    public Integer getRateLimitPerHour() {
        return rateLimitPerHour;
    }
    
    public void setRateLimitPerHour(Integer rateLimitPerHour) {
        this.rateLimitPerHour = rateLimitPerHour;
    }
    
    public String getAllowedIps() {
        return allowedIps;
    }
    
    public void setAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
    }
    
    public String getScopes() {
        return scopes;
    }
    
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (keyValue == null) {
            keyValue = generateApiKey();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public boolean isIpAllowed(String ip) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true; // No IP restriction
        }
        String[] ips = allowedIps.split(",");
        for (String allowedIp : ips) {
            if (allowedIp.trim().equals(ip)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasScope(String scope) {
        if (scopes == null) {
            return false;
        }
        return scopes.contains(scope);
    }
}