package com.saas.platform.dto;

public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Long tenantId;
    private String tenantName;
    private String subdomain;
    
    // 2FA fields
    private Boolean requiresTwoFactor = false;
    private String twoFactorMethod;
    private String message;
    
    // Constructors
    public AuthResponse() {
    }
    
    public AuthResponse(String token, String type, Long userId, String email, 
                       String firstName, String lastName, String role, 
                       Long tenantId, String tenantName, String subdomain) {
        this.token = token;
        this.type = type;
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.subdomain = subdomain;
    }
    
    // Static factory method for 2FA required response
    public static AuthResponse requireTwoFactor(String method, String email) {
        AuthResponse response = new AuthResponse();
        response.setRequiresTwoFactor(true);
        response.setTwoFactorMethod(method);
        response.setEmail(email);
        response.setMessage("Two-factor authentication required");
        return response;
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
    
    public String getSubdomain() {
        return subdomain;
    }
    
    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }
    
    public Boolean getRequiresTwoFactor() {
        return requiresTwoFactor;
    }
    
    public void setRequiresTwoFactor(Boolean requiresTwoFactor) {
        this.requiresTwoFactor = requiresTwoFactor;
    }
    
    public String getTwoFactorMethod() {
        return twoFactorMethod;
    }
    
    public void setTwoFactorMethod(String twoFactorMethod) {
        this.twoFactorMethod = twoFactorMethod;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}