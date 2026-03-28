package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

final class NodeStatusRefreshService {
    private final ProviderDescriptorRegistry descriptorRegistry;
    private final NodeStatusRefreshCoordinator.RefreshDependencies dependencies;

    NodeStatusRefreshService(
            ProviderDescriptorRegistry descriptorRegistry,
            NodeStatusRefreshCoordinator.RefreshDependencies dependencies
    ) {
        this.descriptorRegistry = descriptorRegistry;
        this.dependencies = dependencies;
    }

    @NonNull
    NodeStatusRefreshCoordinator.RefreshOutcome refresh(
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderRefreshConfig config,
            @NonNull String source
    ) {
        if (!config.hasRunnableAuth()) {
            throw new IllegalStateException("No complete authentication flow is configured.");
        }

        ProviderDescriptor descriptor = descriptorRegistry.getDescriptor(siteProfile.getProviderFamily());
        if (!descriptor.supportsRefresh()) {
            throw new IllegalStateException("Refresh is not supported for provider family " + siteProfile.getProviderFamily());
        }
        ProviderRefreshAdapter adapter = descriptor.requireRefreshAdapter();

        dependencies.savePending(
                source,
                "Refresh in progress.",
                dependencies.now().toString()
        );

        Instant collectedAt = dependencies.now();
        List<ResourceSnapshot> snapshotsToStore = applySiteProfile(
                siteProfile,
                adapter.fetchSnapshots(siteProfile, config, collectedAt)
        );
        dependencies.saveLatestSnapshots(siteProfile, snapshotsToStore);
        dependencies.appendHistorySnapshots(snapshotsToStore);

        NodeStatusRefreshCoordinator.RefreshOutcome outcome = new NodeStatusRefreshCoordinator.RefreshOutcome(
                snapshotsToStore.size()
        );
        dependencies.saveSuccess(
                source,
                outcome.toStatusMessage(),
                dependencies.now().toString(),
                outcome.usedCompatibilitySession()
        );
        dependencies.notifyLowTraffic(config, snapshotsToStore);
        dependencies.refreshWidgets();
        dependencies.reconcileRefreshWork(config);
        return outcome;
    }

    @NonNull
    private List<ResourceSnapshot> applySiteProfile(
            @NonNull SiteProfile siteProfile,
            @NonNull List<ResourceSnapshot> snapshots
    ) {
        List<ResourceSnapshot> scopedSnapshots = new ArrayList<>();
        for (ResourceSnapshot snapshot : snapshots) {
            scopedSnapshots.add(snapshot.withSiteProfile(siteProfile));
        }
        return scopedSnapshots;
    }
}
