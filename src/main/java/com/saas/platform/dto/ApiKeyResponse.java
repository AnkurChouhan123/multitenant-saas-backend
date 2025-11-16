package com.saas.platform.dto;

import java.time.LocalDateTime;

public class ApiKeyResponse {
    private Long id;
    private String name;
    private String keyValue;
    private Long tenantId;
    private Boolean isActive;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Long usageCount;
    private Integer rateLimitPerHour;
    private String scopes;
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getKeyValue() { return keyValue; }
    public void setKeyValue(String keyValue) { this.keyValue = keyValue; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    
    public Long getUsageCount() { return usageCount; }
    public void setUsageCount(Long usageCount) { this.usageCount = usageCount; }
    
    public Integer getRateLimitPerHour() { return rateLimitPerHour; }
    public void setRateLimitPerHour(Integer rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
    
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
