package com.saas.platform.dto;

import java.util.List;

public class TwoFactorResponse {
    private boolean success;
    private String message;
    private String secret;
    private String qrCode;
    private String manualEntryKey;
    private String method;
    private List<String> backupCodes;
    private Boolean enabled;
    private Boolean verified;
    
    public TwoFactorResponse() {}
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public String getManualEntryKey() { return manualEntryKey; }
    public void setManualEntryKey(String manualEntryKey) { this.manualEntryKey = manualEntryKey; }
    
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    
    public List<String> getBackupCodes() { return backupCodes; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
}