
package com.saas.platform.service;

import com.saas.platform.dto.AnalyticsDashboardDto;
import com.saas.platform.model.ActivityLog;
import com.saas.platform.model.Subscription;
import com.saas.platform.model.SubscriptionPlan;
import com.saas.platform.repository.ActivityLogRepository;
import com.saas.platform.repository.SubscriptionRepository;
import com.saas.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    public AnalyticsService(ActivityLogRepository activityLogRepository,
                           UserRepository userRepository,
                           SubscriptionRepository subscriptionRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }
    
    public AnalyticsDashboardDto getDashboardMetrics(Long tenantId) {
        log.info("Generating dashboard metrics for tenant ID: {}", tenantId);
        
        AnalyticsDashboardDto dashboard = new AnalyticsDashboardDto();
        
        // Total Users
        long totalUsers = userRepository.countByTenantId(tenantId);
        dashboard.setTotalUsers(totalUsers);
        
        // Total Activities
        List<ActivityLog> allActivities = activityLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        dashboard.setTotalActivities((long) allActivities.size());
        
        // Recent Activities (Last 10)
        List<ActivityLog> recentActivities = allActivities.size() > 10 
            ? allActivities.subList(0, 10) 
            : allActivities;
        dashboard.setRecentActivities(recentActivities);
        
        // Subscription Information
        Optional<Subscription> subscription = subscriptionRepository.findByTenantId(tenantId);
        
        if (subscription.isPresent()) {
            Subscription sub = subscription.get();
            
            dashboard.setCurrentPlan(sub.getPlan().toString());
            dashboard.setSubscriptionActive(sub.getIsActive());
            dashboard.setCurrentApiCalls(sub.getCurrentApiCalls());
            dashboard.setCurrentUsersCount(sub.getCurrentUsers());
            dashboard.setSubscriptionStartDate(sub.getStartDate());
            dashboard.setSubscriptionEndDate(sub.getEndDate());
            
            // Subscription Metrics
            AnalyticsDashboardDto.SubscriptionMetricsDto metrics = 
                buildSubscriptionMetrics(sub);
            dashboard.setSubscriptionMetrics(metrics);
        }
        
        // User Growth
        AnalyticsDashboardDto.UserGrowthDto userGrowth = buildUserGrowth(tenantId, allActivities);
        dashboard.setUserGrowth(userGrowth);
        
        // API Usage
        AnalyticsDashboardDto.ApiUsageDto apiUsage = buildApiUsage(allActivities);
        dashboard.setApiUsage(apiUsage);
        
        log.info("Dashboard metrics generated successfully");
        return dashboard;
    }
    
    private AnalyticsDashboardDto.UserGrowthDto buildUserGrowth(Long tenantId, List<ActivityLog> allActivities) {
        AnalyticsDashboardDto.UserGrowthDto userGrowth = new AnalyticsDashboardDto.UserGrowthDto();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMonth = now.minus(30, ChronoUnit.DAYS);
        LocalDateTime lastWeek = now.minus(7, ChronoUnit.DAYS);
        
        // Count user creation activities
        long monthlyNewUsers = allActivities.stream()
            .filter(a -> a.getCreatedAt().isAfter(lastMonth) && a.getAction().contains("created"))
            .count();
        
        long weeklyNewUsers = allActivities.stream()
            .filter(a -> a.getCreatedAt().isAfter(lastWeek) && a.getAction().contains("created"))
            .count();
        
        long totalUsers = userRepository.countByTenantId(tenantId);
        double growthPercentage = totalUsers > 0 ? (weeklyNewUsers / (double) totalUsers) * 100 : 0;
        
        userGrowth.setMonthlyUsers((int) monthlyNewUsers);
        userGrowth.setWeeklyUsers((int) weeklyNewUsers);
        userGrowth.setTotalUsers((int) totalUsers);
        userGrowth.setGrowthPercentage(growthPercentage);
        
        return userGrowth;
    }
    
    private AnalyticsDashboardDto.ApiUsageDto buildApiUsage(List<ActivityLog> allActivities) {
        AnalyticsDashboardDto.ApiUsageDto apiUsage = new AnalyticsDashboardDto.ApiUsageDto();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime lastWeek = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime lastMonth = now.minus(30, ChronoUnit.DAYS);
        
        long dailyApiCalls = allActivities.stream()
            .filter(a -> a.getCreatedAt().isAfter(today))
            .count();
        
        long weeklyApiCalls = allActivities.stream()
            .filter(a -> a.getCreatedAt().isAfter(lastWeek))
            .count();
        
        long monthlyApiCalls = allActivities.stream()
            .filter(a -> a.getCreatedAt().isAfter(lastMonth))
            .count();
        
        apiUsage.setDailyApiCalls((int) dailyApiCalls);
        apiUsage.setWeeklyApiCalls((int) weeklyApiCalls);
        apiUsage.setMonthlyApiCalls((int) monthlyApiCalls);
        apiUsage.setAverageResponseTime(145.0); // Mock data
        apiUsage.setErrorRate(0.02); // Mock data (0.02%)
        
        return apiUsage;
    }
    
    private AnalyticsDashboardDto.SubscriptionMetricsDto buildSubscriptionMetrics(Subscription subscription) {
        AnalyticsDashboardDto.SubscriptionMetricsDto metrics = 
            new AnalyticsDashboardDto.SubscriptionMetricsDto();
        
        SubscriptionPlan plan = subscription.getPlan();
        
        metrics.setPlanName(plan.toString());
        metrics.setMaxUsers(plan.getMaxUsers() == -1 ? 999999 : plan.getMaxUsers());
        metrics.setMaxApiCalls(plan.getMaxApiCalls() == -1 ? 999999 : plan.getMaxApiCalls());
        metrics.setUsedUsers(subscription.getCurrentUsers());
        metrics.setUsedApiCalls(subscription.getCurrentApiCalls());
        
        // Calculate percentages
        int usersPercentage = plan.getMaxUsers() == -1 ? 0 : 
            (subscription.getCurrentUsers() * 100) / plan.getMaxUsers();
        
        int apiCallsPercentage = plan.getMaxApiCalls() == -1 ? 0 : 
            (subscription.getCurrentApiCalls() * 100) / plan.getMaxApiCalls();
        
        metrics.setUsersPercentage(Math.min(usersPercentage, 100));
        metrics.setApiCallsPercentage(Math.min(apiCallsPercentage, 100));
        
        // Calculate days remaining
        if (subscription.getEndDate() != null) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), 
                subscription.getEndDate());
            metrics.setDaysRemaining((int) daysRemaining);
        } else {
            metrics.setDaysRemaining(-1); // Unlimited
        }
        
        return metrics;
    }
    
    public List<ActivityLog> getActivitiesByDateRange(Long tenantId, LocalDateTime start, 
                                                      LocalDateTime end) {
        return activityLogRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end);
    }
    
    public List<ActivityLog> getActivitiesByType(Long tenantId, String actionType) {
        return activityLogRepository.findByTenantIdAndActionType(tenantId, actionType);
    }
}
