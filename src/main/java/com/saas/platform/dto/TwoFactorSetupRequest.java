package com.saas.platform.dto;

public class TwoFactorSetupRequest {
    private String method; // TOTP or EMAIL
    
    public TwoFactorSetupRequest() {}
    
    public TwoFactorSetupRequest(String method) {
        this.method = method;
    }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}