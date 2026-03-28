package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.util.List;
import java.time.Instant;

final class ExampleProviderRefreshAdapter implements ProviderRefreshAdapter {
    @Override
    public @NonNull List<ResourceSnapshot> fetchSnapshots(
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderRefreshConfig config,
            @NonNull Instant collectedAt
    ) {
        throw new UnsupportedOperationException("Example provider is a placeholder and does not implement refresh yet.");
    }
}
