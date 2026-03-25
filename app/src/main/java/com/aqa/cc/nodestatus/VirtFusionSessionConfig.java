package com.aqa.cc.nodestatus;

public final class VirtFusionSessionConfig {
    private final String baseUrl;
    private final String apiBaseUrl;
    private final String apiToken;
    private final String cookieHeader;
    private final String xsrfHeader;
    private final String userAgent;
    private final boolean allowInsecureTls;
    private final boolean notificationsEnabled;
    private final int lowTrafficThresholdPercent;

    public VirtFusionSessionConfig(
            String baseUrl,
            String apiBaseUrl,
            String apiToken,
            String cookieHeader,
            String xsrfHeader,
            String userAgent,
            boolean allowInsecureTls,
            boolean notificationsEnabled,
            int lowTrafficThresholdPercent
    ) {
        this.baseUrl = baseUrl;
        this.apiBaseUrl = apiBaseUrl;
        this.apiToken = apiToken;
        this.cookieHeader = cookieHeader;
        this.xsrfHeader = xsrfHeader;
        this.userAgent = userAgent;
        this.allowInsecureTls = allowInsecureTls;
        this.notificationsEnabled = notificationsEnabled;
        this.lowTrafficThresholdPercent = lowTrafficThresholdPercent;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getCookieHeader() {
        return cookieHeader;
    }

    public String getXsrfHeader() {
        return xsrfHeader;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean isAllowInsecureTls() {
        return allowInsecureTls;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public int getLowTrafficThresholdPercent() {
        return lowTrafficThresholdPercent;
    }

    public boolean hasApiToken() {
        return !apiToken.isBlank();
    }

    public boolean hasCompatibilitySession() {
        return !cookieHeader.isBlank() && !xsrfHeader.isBlank();
    }
}
