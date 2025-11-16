package com.saas.platform.dto;
class WebhookCreateRequest {
    private String name;
    private String url;
    private String events;
    private Boolean isActive;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getEvents() { return events; }
    public void setEvents(String events) { this.events = events; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}