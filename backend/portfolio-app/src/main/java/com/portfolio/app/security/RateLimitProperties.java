package com.portfolio.app.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Requests allowed per client IP per minute. */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int login = 10;
    private int register = 5;
    private int refresh = 30;
    private int comment = 10;
    private int general = 300;
    /** Requests with a larger body are rejected with 413 before they are parsed. */
    private long maxBodyBytes = 1_048_576;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getLogin() { return login; }
    public void setLogin(int login) { this.login = login; }
    public int getRegister() { return register; }
    public void setRegister(int register) { this.register = register; }
    public int getRefresh() { return refresh; }
    public void setRefresh(int refresh) { this.refresh = refresh; }
    public int getComment() { return comment; }
    public void setComment(int comment) { this.comment = comment; }
    public int getGeneral() { return general; }
    public void setGeneral(int general) { this.general = general; }
    public long getMaxBodyBytes() { return maxBodyBytes; }
    public void setMaxBodyBytes(long maxBodyBytes) { this.maxBodyBytes = maxBodyBytes; }
}
