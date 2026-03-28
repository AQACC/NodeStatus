package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class AppRefreshDependencies implements NodeStatusRefreshCoordinator.RefreshDependencies {
    private final Context appContext;

    AppRefreshDependencies(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public @NonNull Instant now() {
        return Instant.now();
    }

    @Override
    public void savePending(@NonNull String source, @NonNull String message, @NonNull String updatedAt) {
        new NodeStatusRefreshStatusStore(appContext).savePending(source, message, updatedAt);
    }

    @Override
    public void saveSuccess(
            @NonNull String source,
            @NonNull String message,
            @NonNull String updatedAt,
            boolean usedSession
    ) {
        new NodeStatusRefreshStatusStore(appContext).saveSuccess(
                source,
                message,
                updatedAt,
                usedSession
        );
    }

    @Override
    public void saveLatestSnapshots(@NonNull SiteProfile siteProfile, @NonNull List<ResourceSnapshot> snapshots) {
        FileSnapshotStore store = new FileSnapshotStore(AppSnapshotFiles.latestSnapshotPath(appContext));
        List<ResourceSnapshot> merged = new ArrayList<>();
        for (ResourceSnapshot snapshot : store.listLatest()) {
            if (!belongsToSite(siteProfile, snapshot)) {
                merged.add(snapshot);
            }
        }
        merged.addAll(snapshots);
        store.saveAll(merged);
    }

    @Override
    public void appendHistorySnapshots(@NonNull List<ResourceSnapshot> snapshots) {
        new FileSnapshotHistoryStore(AppSnapshotFiles.historySnapshotPath(appContext), 240).appendAll(snapshots);
    }

    @Override
    public void notifyLowTraffic(@NonNull ProviderRefreshConfig config, @NonNull List<ResourceSnapshot> snapshots) {
        NodeStatusNotificationHelper.maybeNotifyLowTraffic(appContext, config, snapshots);
    }

    @Override
    public void refreshWidgets() {
        NodeStatusWidgetProvider.refreshAll(appContext);
    }

    @Override
    public void reconcileRefreshWork(@NonNull ProviderRefreshConfig config) {
        NodeStatusRefreshCoordinator.reconcileRefreshWork(appContext, config);
    }

    private boolean belongsToSite(@NonNull SiteProfile siteProfile, @NonNull ResourceSnapshot snapshot) {
        if (siteProfile.getId().equals(snapshot.getSiteId())) {
            return true;
        }
        return SiteCatalogStore.DEFAULT_SITE_ID.equals(siteProfile.getId())
                && snapshot.getSiteId().isBlank();
    }

}
