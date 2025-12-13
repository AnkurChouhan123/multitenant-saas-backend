package com.saas.platform.dto;
public class SubscriptionPlanDto {
    private Long id;
    private String name;
    private Double monthlyPrice;
    private Integer maxUsers;
    private Integer maxApiCalls;
    private Integer maxStorageGB;
    private Boolean isUnlimited;
    private Long tenantCount;
    private Boolean isActive;
    
    // Constructors
    public SubscriptionPlanDto() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Double getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(Double monthlyPrice) { this.monthlyPrice = monthlyPrice; }
    
    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
    
    public Integer getMaxApiCalls() { return maxApiCalls; }
    public void setMaxApiCalls(Integer maxApiCalls) { this.maxApiCalls = maxApiCalls; }
    
    public Integer getMaxStorageGB() { return maxStorageGB; }
    public void setMaxStorageGB(Integer maxStorageGB) { this.maxStorageGB = maxStorageGB; }
    
    public Boolean getIsUnlimited() { return isUnlimited; }
    public void setIsUnlimited(Boolean isUnlimited) { this.isUnlimited = isUnlimited; }
    
    public Long getTenantCount() { return tenantCount; }
    public void setTenantCount(Long tenantCount) { this.tenantCount = tenantCount; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

// =========================================

