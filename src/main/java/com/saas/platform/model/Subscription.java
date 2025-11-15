package com.saas.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Subscription Entity - Tracks tenant's subscription details
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;
    
    @Column(name = "current_users")
    private Integer currentUsers = 0;
    
    @Column(name = "current_api_calls")
    private Integer currentApiCalls = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Subscription() {
    }
    
    public Subscription(Long id, Tenant tenant, SubscriptionPlan plan, 
                       LocalDateTime startDate, LocalDateTime endDate, 
                       Boolean isActive, Boolean autoRenew, 
                       Integer currentUsers, Integer currentApiCalls,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tenant = tenant;
        this.plan = plan;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.autoRenew = autoRenew;
        this.currentUsers = currentUsers;
        this.currentApiCalls = currentApiCalls;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Tenant getTenant() {
        return tenant;
    }
    
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
    
    public SubscriptionPlan getPlan() {
        return plan;
    }
    
    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getAutoRenew() {
        return autoRenew;
    }
    
    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }
    
    public Integer getCurrentUsers() {
        return currentUsers;
    }
    
    public void setCurrentUsers(Integer currentUsers) {
        this.currentUsers = currentUsers;
    }
    
    public Integer getCurrentApiCalls() {
        return currentApiCalls;
    }
    
    public void setCurrentApiCalls(Integer currentApiCalls) {
        this.currentApiCalls = currentApiCalls;
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
        startDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if subscription has expired
     */
    public boolean isExpired() {
        return endDate != null && LocalDateTime.now().isAfter(endDate);
    }
    
    /**
     * Check if user limit is reached
     */
    public boolean hasReachedUserLimit() {
        if (plan.isUnlimited()) return false;
        return currentUsers >= plan.getMaxUsers();
    }
    
    /**
     * Check if API call limit is reached
     */
    public boolean hasReachedApiLimit() {
        if (plan.isUnlimited()) return false;
        return currentApiCalls >= plan.getMaxApiCalls();
    }
}