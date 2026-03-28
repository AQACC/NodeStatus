package com.aqa.cc.nodestatus;

import java.util.Objects;

public class ProviderSessionConfig implements ProviderRefreshConfig {
    private final String baseUrl;
    private final String loginEntryUrl;
    private final String cookieHeader;
    private final String xsrfHeader;
    private final String userAgent;
    private final boolean allowInsecureTls;
    private final boolean notificationsEnabled;
    private final int lowTrafficThresholdPercent;

    public ProviderSessionConfig(
            String baseUrl,
            String loginEntryUrl,
            String cookieHeader,
            String xsrfHeader,
            String userAgent,
            boolean allowInsecureTls,
            boolean notificationsEnabled,
            int lowTrafficThresholdPercent
    ) {
        this.baseUrl = baseUrl;
        this.loginEntryUrl = loginEntryUrl;
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

    public String getLoginEntryUrl() {
        return loginEntryUrl;
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
        return BuildConfig.DEBUG && allowInsecureTls;
    }

    @Override
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    @Override
    public int getLowTrafficThresholdPercent() {
        return lowTrafficThresholdPercent;
    }

    public boolean hasCompatibilitySession() {
        return !cookieHeader.isBlank();
    }

    public boolean canUseCompatibilitySessionFlow() {
        return hasCompatibilitySession() && !baseUrl.isBlank();
    }

    @Override
    public boolean hasAnyAuth() {
        return hasCompatibilitySession();
    }

    @Override
    public boolean hasRunnableAuth() {
        return canUseCompatibilitySessionFlow();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProviderSessionConfig)) {
            return false;
        }
        ProviderSessionConfig that = (ProviderSessionConfig) other;
        return allowInsecureTls == that.allowInsecureTls
                && notificationsEnabled == that.notificationsEnabled
                && lowTrafficThresholdPercent == that.lowTrafficThresholdPercent
                && Objects.equals(baseUrl, that.baseUrl)
                && Objects.equals(loginEntryUrl, that.loginEntryUrl)
                && Objects.equals(cookieHeader, that.cookieHeader)
                && Objects.equals(xsrfHeader, that.xsrfHeader)
                && Objects.equals(userAgent, that.userAgent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                baseUrl,
                loginEntryUrl,
                cookieHeader,
                xsrfHeader,
                userAgent,
                allowInsecureTls,
                notificationsEnabled,
                lowTrafficThresholdPercent
        );
    }
}
