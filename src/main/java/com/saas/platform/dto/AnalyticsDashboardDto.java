
package com.saas.platform.dto;

import com.saas.platform.model.ActivityLog;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;

public class AnalyticsDashboardDto {
    
    private Long totalUsers;
    private Long totalActivities;
    private String currentPlan;
    private Boolean subscriptionActive;
    private Integer currentApiCalls;
    private Integer currentUsersCount;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private List<ActivityLog> recentActivities;
    private UserGrowthDto userGrowth;
    private ApiUsageDto apiUsage;
    private SubscriptionMetricsDto subscriptionMetrics;
    
    // Constructors
    public AnalyticsDashboardDto() {
    }
    
    public AnalyticsDashboardDto(
            Long totalUsers,
            Long totalActivities,
            String currentPlan,
            Boolean subscriptionActive,
            Integer currentApiCalls,
            Integer currentUsersCount,
            LocalDateTime subscriptionStartDate,
            LocalDateTime subscriptionEndDate,
            List<ActivityLog> recentActivities,
            UserGrowthDto userGrowth,
            ApiUsageDto apiUsage,
            SubscriptionMetricsDto subscriptionMetrics) {
        this.totalUsers = totalUsers;
        this.totalActivities = totalActivities;
        this.currentPlan = currentPlan;
        this.subscriptionActive = subscriptionActive;
        this.currentApiCalls = currentApiCalls;
        this.currentUsersCount = currentUsersCount;
        this.subscriptionStartDate = subscriptionStartDate;
        this.subscriptionEndDate = subscriptionEndDate;
        this.recentActivities = recentActivities;
        this.userGrowth = userGrowth;
        this.apiUsage = apiUsage;
        this.subscriptionMetrics = subscriptionMetrics;
    }
    
    // Getters and Setters
    public Long getTotalUsers() {
        return totalUsers;
    }
    
    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }
    
    public Long getTotalActivities() {
        return totalActivities;
    }
    
    public void setTotalActivities(Long totalActivities) {
        this.totalActivities = totalActivities;
    }
    
    public String getCurrentPlan() {
        return currentPlan;
    }
    
    public void setCurrentPlan(String currentPlan) {
        this.currentPlan = currentPlan;
    }
    
    public Boolean getSubscriptionActive() {
        return subscriptionActive;
    }
    
    public void setSubscriptionActive(Boolean subscriptionActive) {
        this.subscriptionActive = subscriptionActive;
    }
    
    public Integer getCurrentApiCalls() {
        return currentApiCalls;
    }
    
    public void setCurrentApiCalls(Integer currentApiCalls) {
        this.currentApiCalls = currentApiCalls;
    }
    
    public Integer getCurrentUsersCount() {
        return currentUsersCount;
    }
    
    public void setCurrentUsersCount(Integer currentUsersCount) {
        this.currentUsersCount = currentUsersCount;
    }
    
    public LocalDateTime getSubscriptionStartDate() {
        return subscriptionStartDate;
    }
    
    public void setSubscriptionStartDate(LocalDateTime subscriptionStartDate) {
        this.subscriptionStartDate = subscriptionStartDate;
    }
    
    public LocalDateTime getSubscriptionEndDate() {
        return subscriptionEndDate;
    }
    
    public void setSubscriptionEndDate(LocalDateTime subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }
    
    public List<ActivityLog> getRecentActivities() {
        return recentActivities;
    }
    
    public void setRecentActivities(List<ActivityLog> recentActivities) {
        this.recentActivities = recentActivities;
    }
    
    public UserGrowthDto getUserGrowth() {
        return userGrowth;
    }
    
    public void setUserGrowth(UserGrowthDto userGrowth) {
        this.userGrowth = userGrowth;
    }
    
    public ApiUsageDto getApiUsage() {
        return apiUsage;
    }
    
    public void setApiUsage(ApiUsageDto apiUsage) {
        this.apiUsage = apiUsage;
    }
    
    public SubscriptionMetricsDto getSubscriptionMetrics() {
        return subscriptionMetrics;
    }
    
    public void setSubscriptionMetrics(SubscriptionMetricsDto subscriptionMetrics) {
        this.subscriptionMetrics = subscriptionMetrics;
    }
    
    // Inner DTO Classes
    
    public static class UserGrowthDto {
        private Integer monthlyUsers;
        private Integer weeklyUsers;
        private Integer totalUsers;
        private Double growthPercentage;
        
        public UserGrowthDto() {
        }
        
        public UserGrowthDto(Integer monthlyUsers, Integer weeklyUsers, 
                            Integer totalUsers, Double growthPercentage) {
            this.monthlyUsers = monthlyUsers;
            this.weeklyUsers = weeklyUsers;
            this.totalUsers = totalUsers;
            this.growthPercentage = growthPercentage;
        }
        
        public Integer getMonthlyUsers() { return monthlyUsers; }
        public void setMonthlyUsers(Integer monthlyUsers) { this.monthlyUsers = monthlyUsers; }
        
        public Integer getWeeklyUsers() { return weeklyUsers; }
        public void setWeeklyUsers(Integer weeklyUsers) { this.weeklyUsers = weeklyUsers; }
        
        public Integer getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }
        
        public Double getGrowthPercentage() { return growthPercentage; }
        public void setGrowthPercentage(Double growthPercentage) { this.growthPercentage = growthPercentage; }
    }
    
    public static class ApiUsageDto {
        private Integer dailyApiCalls;
        private Integer weeklyApiCalls;
        private Integer monthlyApiCalls;
        private Double averageResponseTime;
        private Double errorRate;
        
        public ApiUsageDto() {
        }
        
        public ApiUsageDto(Integer dailyApiCalls, Integer weeklyApiCalls, 
                          Integer monthlyApiCalls, Double averageResponseTime, 
                          Double errorRate) {
            this.dailyApiCalls = dailyApiCalls;
            this.weeklyApiCalls = weeklyApiCalls;
            this.monthlyApiCalls = monthlyApiCalls;
            this.averageResponseTime = averageResponseTime;
            this.errorRate = errorRate;
        }
        
        public Integer getDailyApiCalls() { return dailyApiCalls; }
        public void setDailyApiCalls(Integer dailyApiCalls) { this.dailyApiCalls = dailyApiCalls; }
        
        public Integer getWeeklyApiCalls() { return weeklyApiCalls; }
        public void setWeeklyApiCalls(Integer weeklyApiCalls) { this.weeklyApiCalls = weeklyApiCalls; }
        
        public Integer getMonthlyApiCalls() { return monthlyApiCalls; }
        public void setMonthlyApiCalls(Integer monthlyApiCalls) { this.monthlyApiCalls = monthlyApiCalls; }
        
        public Double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(Double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
    }
    
    public static class SubscriptionMetricsDto {
        private String planName;
        private Integer maxUsers;
        private Integer maxApiCalls;
        private Integer usedUsers;
        private Integer usedApiCalls;
        private Integer usersPercentage;
        private Integer apiCallsPercentage;
        private Integer daysRemaining;
        
        public SubscriptionMetricsDto() {
        }
        
        public SubscriptionMetricsDto(String planName, Integer maxUsers, 
                                     Integer maxApiCalls, Integer usedUsers, 
                                     Integer usedApiCalls, Integer usersPercentage, 
                                     Integer apiCallsPercentage, Integer daysRemaining) {
            this.planName = planName;
            this.maxUsers = maxUsers;
            this.maxApiCalls = maxApiCalls;
            this.usedUsers = usedUsers;
            this.usedApiCalls = usedApiCalls;
            this.usersPercentage = usersPercentage;
            this.apiCallsPercentage = apiCallsPercentage;
            this.daysRemaining = daysRemaining;
        }
        
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
        
        public Integer getMaxUsers() { return maxUsers; }
        public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }
        
        public Integer getMaxApiCalls() { return maxApiCalls; }
        public void setMaxApiCalls(Integer maxApiCalls) { this.maxApiCalls = maxApiCalls; }
        
        public Integer getUsedUsers() { return usedUsers; }
        public void setUsedUsers(Integer usedUsers) { this.usedUsers = usedUsers; }
        
        public Integer getUsedApiCalls() { return usedApiCalls; }
        public void setUsedApiCalls(Integer usedApiCalls) { this.usedApiCalls = usedApiCalls; }
        
        public Integer getUsersPercentage() { return usersPercentage; }
        public void setUsersPercentage(Integer usersPercentage) { this.usersPercentage = usersPercentage; }
        
        public Integer getApiCallsPercentage() { return apiCallsPercentage; }
        public void setApiCallsPercentage(Integer apiCallsPercentage) { this.apiCallsPercentage = apiCallsPercentage; }
        
        public Integer getDaysRemaining() { return daysRemaining; }
        public void setDaysRemaining(Integer daysRemaining) { this.daysRemaining = daysRemaining; }
    }
}