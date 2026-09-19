package com.sentinel.aml.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sentinel.security")
public class SecurityUserProperties {
    private String adminUsername;
    private String adminPassword;
    private String analystUsername;
    private String analystPassword;

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String value) { adminUsername = value; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String value) { adminPassword = value; }
    public String getAnalystUsername() { return analystUsername; }
    public void setAnalystUsername(String value) { analystUsername = value; }
    public String getAnalystPassword() { return analystPassword; }
    public void setAnalystPassword(String value) { analystPassword = value; }
}