package com.saas.platform.model;

//
// Tenant Status Enum
 
public enum TenantStatus {
    TRIAL,      // Free trial period
    ACTIVE,     // Paid and active
    SUSPENDED,  // Payment failed or violated terms
    CANCELLED   // Tenant cancelled subscription
}