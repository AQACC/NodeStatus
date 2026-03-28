package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.time.Instant;
import java.util.List;

interface ProviderRefreshAdapter {
    @NonNull
    List<ResourceSnapshot> fetchSnapshots(
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderRefreshConfig config,
            @NonNull Instant collectedAt
    );
}
