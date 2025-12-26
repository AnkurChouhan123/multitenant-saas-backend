package com.saas.platform.dto;

//
// PasswordResetRequest DTO
 
public class PasswordResetRequest {
    private String email;
    
    public PasswordResetRequest() {}
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}