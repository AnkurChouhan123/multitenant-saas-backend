package com.saas.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
public class Plan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private Double monthlyPrice;
    
    @Column(nullable = false)
    private Integer maxUsers;
    
    @Column(nullable = false)
    private Integer maxApiCalls;
    
    @Column(nullable = false)
    private Integer maxStorageGB;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(nullable = false)
    private Boolean isCustom = false; // true = created by admin, false = default
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Plan() {}
    
    public Plan(String name, Double monthlyPrice, Integer maxUsers, 
                Integer maxApiCalls, Integer maxStorageGB) {
        this.name = name;
        this.monthlyPrice = monthlyPrice;
        this.maxUsers = maxUsers;
        this.maxApiCalls = maxApiCalls;
        this.maxStorageGB = maxStorageGB;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }
    
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
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getIsCustom() { return isCustom; }
    public void setIsCustom(Boolean isCustom) { this.isCustom = isCustom; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean isUnlimited() {
        return maxUsers == -1 || maxApiCalls == -1;
    }
}