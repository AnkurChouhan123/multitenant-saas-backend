package com.saas.platform.dto;

public class TwoFactorVerifyRequest {
    private String code;
    
    public TwoFactorVerifyRequest() {}
    
    public TwoFactorVerifyRequest(String code) {
        this.code = code;
    }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
