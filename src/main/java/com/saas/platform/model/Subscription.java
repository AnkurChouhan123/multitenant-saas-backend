package com.saas.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    // CHANGED: Use Plan entity instead of enum
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;
    
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "renewal_date")
    private LocalDateTime renewalDate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "auto_renew")
    private Boolean autoRenew = false;
    
    @Column(name = "current_users")
    private Integer currentUsers = 0;
    
    @Column(name = "current_api_calls")
    private Integer currentApiCalls = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Business logic methods
    
    public boolean isExpired() {
        if (endDate == null) return false;
        return LocalDateTime.now().isAfter(endDate);
    }
    
    public boolean hasReachedUserLimit() {
        if (plan == null) return false;
        // -1 means unlimited
        if (plan.getMaxUsers() == -1) return false;
        return currentUsers >= plan.getMaxUsers();
    }
    
    public boolean hasReachedApiLimit() {
        if (plan == null) return false;
        // -1 means unlimited
        if (plan.getMaxApiCalls() == -1) return false;
        return currentApiCalls >= plan.getMaxApiCalls();
    }
    
    public boolean hasReachedStorageLimit(Integer currentStorageGB) {
        if (plan == null) return false;
        // -1 means unlimited
        if (plan.getMaxStorageGB() == -1) return false;
        return currentStorageGB >= plan.getMaxStorageGB();
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
    
    public Plan getPlan() {
        return plan;
    }
    
    public void setPlan(Plan plan) {
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
    
    public LocalDateTime getRenewalDate() {
        return renewalDate;
    }
    
    public void setRenewalDate(LocalDateTime renewalDate) {
        this.renewalDate = renewalDate;
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
}