package com.saas.platform.model;

//
// Subscription Plans with pricing and limits
 
public enum SubscriptionPlan {
    
    FREE(0.0, 5, 1000),
    BASIC(29.99, 25, 10000),
    PRO(99.99, 100, 50000),
    ENTERPRISE(299.99, -1, -1);  // -1 means unlimited
    
    private final double monthlyPrice;
    private final int maxUsers;
    private final int maxApiCalls;
    
    SubscriptionPlan(double monthlyPrice, int maxUsers, int maxApiCalls) {
        this.monthlyPrice = monthlyPrice;
        this.maxUsers = maxUsers;
        this.maxApiCalls = maxApiCalls;
    }
    
    public double getMonthlyPrice() {
        return monthlyPrice;
    }
    
    public int getMaxUsers() {
        return maxUsers;
    }
    
    public int getMaxApiCalls() {
        return maxApiCalls;
    }
    
    public boolean isUnlimited() {
        return maxUsers == -1;
    }
}