package com.saas.platform.dto;

// ===== API Key DTOs =====

public class ApiKeyCreateRequest {
    private Long userId;
    private String name;
    private String scopes;
    private Integer expiresInDays;
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    
    public Integer getExpiresInDays() { return expiresInDays; }
    public void setExpiresInDays(Integer expiresInDays) { this.expiresInDays = expiresInDays; }
}