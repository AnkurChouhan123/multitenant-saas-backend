package com.saas.platform.dto;

import java.time.LocalDateTime;

//
// PlatformStatsDto - Platform-wide statistics for Super Admin
 
public class PlatformStatsDto {
    // Tenant metrics
    private Long totalTenants;
    private Long activeTenants;
    private Long trialTenants;
    private Long suspendedTenants;
    
    // User metrics
    private Long totalUsers;
    private Long dau; // Daily Active Users
    private Long mau; // Monthly Active Users
    
    // Revenue metrics
    private Double mrr; // Monthly Recurring Revenue
    private Double totalRevenue;
    
    // Platform usage
    private Long totalApiCalls;
    private Double totalStorageGB;
    private Long totalWebhooks;
    
    // Health metrics
    private Double uptime;
    private Double errorRate;
    
    // Constructors
    public PlatformStatsDto() {}
    
    // Getters and Setters
    public Long getTotalTenants() { return totalTenants; }
    public void setTotalTenants(Long totalTenants) { this.totalTenants = totalTenants; }
    
    public Long getActiveTenants() { return activeTenants; }
    public void setActiveTenants(Long activeTenants) { this.activeTenants = activeTenants; }
    
    public Long getTrialTenants() { return trialTenants; }
    public void setTrialTenants(Long trialTenants) { this.trialTenants = trialTenants; }
    
    public Long getSuspendedTenants() { return suspendedTenants; }
    public void setSuspendedTenants(Long suspendedTenants) { this.suspendedTenants = suspendedTenants; }
    
    public Long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
    
    public Long getDau() { return dau; }
    public void setDau(Long dau) { this.dau = dau; }
    
    public Long getMau() { return mau; }
    public void setMau(Long mau) { this.mau = mau; }
    
    public Double getMrr() { return mrr; }
    public void setMrr(Double mrr) { this.mrr = mrr; }
    
    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }
    
    public Long getTotalApiCalls() { return totalApiCalls; }
    public void setTotalApiCalls(Long totalApiCalls) { this.totalApiCalls = totalApiCalls; }
    
    public Double getTotalStorageGB() { return totalStorageGB; }
    public void setTotalStorageGB(Double totalStorageGB) { this.totalStorageGB = totalStorageGB; }
    
    public Long getTotalWebhooks() { return totalWebhooks; }
    public void setTotalWebhooks(Long totalWebhooks) { this.totalWebhooks = totalWebhooks; }
    
    public Double getUptime() { return uptime; }
    public void setUptime(Double uptime) { this.uptime = uptime; }
    
    public Double getErrorRate() { return errorRate; }
    public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
}

// =========================================





