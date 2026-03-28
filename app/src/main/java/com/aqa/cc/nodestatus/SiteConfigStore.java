package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface SiteConfigStore {
    @NonNull
    ProviderSessionConfig load();

    @NonNull
    ProviderSessionConfig load(@NonNull String siteId);

    void save(@NonNull ProviderSessionConfig config);

    void save(@NonNull String siteId, @NonNull ProviderSessionConfig config);

    boolean hasStoredConfig(@Nullable String siteId);
}
