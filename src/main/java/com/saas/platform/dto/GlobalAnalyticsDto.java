package com.saas.platform.dto;


public class GlobalAnalyticsDto {
    // Growth metrics
    private Long newTenantsLast30Days;
    private Long newUsersLast30Days;
    private Double revenueGrowthPercent;
    private Double userGrowthPercent;
    
    // Usage metrics
    private Long totalApiCalls;
    private Long totalWebhookCalls;
    private Double totalStorageGB;
    private Long totalFileUploads;
    
    // Engagement metrics
    private Long dau;
    private Long mau;
    private Double averageSessionTime;
    private Long activeTenantsToday;
    
    // Revenue metrics
    private Double mrr;
    private Double arr;
    private Double averageRevenuePerTenant;
    private Double churnRate;
    
    // Constructors
    public GlobalAnalyticsDto() {}
    
    // Getters and Setters
    public Long getNewTenantsLast30Days() { return newTenantsLast30Days; }
    public void setNewTenantsLast30Days(Long newTenantsLast30Days) { 
        this.newTenantsLast30Days = newTenantsLast30Days; 
    }
    
    public Long getNewUsersLast30Days() { return newUsersLast30Days; }
    public void setNewUsersLast30Days(Long newUsersLast30Days) { 
        this.newUsersLast30Days = newUsersLast30Days; 
    }
    
    public Double getRevenueGrowthPercent() { return revenueGrowthPercent; }
    public void setRevenueGrowthPercent(Double revenueGrowthPercent) { 
        this.revenueGrowthPercent = revenueGrowthPercent; 
    }
    
    public Double getUserGrowthPercent() { return userGrowthPercent; }
    public void setUserGrowthPercent(Double userGrowthPercent) { 
        this.userGrowthPercent = userGrowthPercent; 
    }
    
    public Long getTotalApiCalls() { return totalApiCalls; }
    public void setTotalApiCalls(Long totalApiCalls) { this.totalApiCalls = totalApiCalls; }
    
    public Long getTotalWebhookCalls() { return totalWebhookCalls; }
    public void setTotalWebhookCalls(Long totalWebhookCalls) { 
        this.totalWebhookCalls = totalWebhookCalls; 
    }
    
    public Double getTotalStorageGB() { return totalStorageGB; }
    public void setTotalStorageGB(Double totalStorageGB) { this.totalStorageGB = totalStorageGB; }
    
    public Long getTotalFileUploads() { return totalFileUploads; }
    public void setTotalFileUploads(Long totalFileUploads) { 
        this.totalFileUploads = totalFileUploads; 
    }
    
    public Long getDau() { return dau; }
    public void setDau(Long dau) { this.dau = dau; }
    
    public Long getMau() { return mau; }
    public void setMau(Long mau) { this.mau = mau; }
    
    public Double getAverageSessionTime() { return averageSessionTime; }
    public void setAverageSessionTime(Double averageSessionTime) { 
        this.averageSessionTime = averageSessionTime; 
    }
    
    public Long getActiveTenantsToday() { return activeTenantsToday; }
    public void setActiveTenantsToday(Long activeTenantsToday) { 
        this.activeTenantsToday = activeTenantsToday; 
    }
    
    public Double getMrr() { return mrr; }
    public void setMrr(Double mrr) { this.mrr = mrr; }
    
    public Double getArr() { return arr; }
    public void setArr(Double arr) { this.arr = arr; }
    
    public Double getAverageRevenuePerTenant() { return averageRevenuePerTenant; }
    public void setAverageRevenuePerTenant(Double averageRevenuePerTenant) { 
        this.averageRevenuePerTenant = averageRevenuePerTenant; 
    }
    
    public Double getChurnRate() { return churnRate; }
    public void setChurnRate(Double churnRate) { this.churnRate = churnRate; }
}