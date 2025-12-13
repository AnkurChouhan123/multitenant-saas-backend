package com.saas.platform.dto;
import java.time.LocalDateTime;

public class TenantManagementDto {
    private Long id;
    private String name;
    private String subdomain;
    private String status;
    private String plan;
    private Boolean subscriptionActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
    
    // Aggregate metrics (not internal data)
    private Long userCount;
    private Double storageUsedGB;
    private Long apiCallCount;
    
    // Constructors
    public TenantManagementDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    
    public Boolean getSubscriptionActive() { return subscriptionActive; }
    public void setSubscriptionActive(Boolean subscriptionActive) { 
        this.subscriptionActive = subscriptionActive; 
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
    
    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    
    public Double getStorageUsedGB() { return storageUsedGB; }
    public void setStorageUsedGB(Double storageUsedGB) { this.storageUsedGB = storageUsedGB; }
    
    public Long getApiCallCount() { return apiCallCount; }
    public void setApiCallCount(Long apiCallCount) { this.apiCallCount = apiCallCount; }
}