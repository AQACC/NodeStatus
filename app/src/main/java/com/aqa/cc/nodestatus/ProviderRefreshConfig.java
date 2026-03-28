package com.aqa.cc.nodestatus;

interface ProviderRefreshConfig {
    boolean hasAnyAuth();

    boolean hasRunnableAuth();

    boolean isNotificationsEnabled();

    int getLowTrafficThresholdPercent();
}
