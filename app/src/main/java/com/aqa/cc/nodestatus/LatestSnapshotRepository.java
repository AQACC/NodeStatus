package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore;
import com.aqa.cc.nodestatus.core.storage.SnapshotStore;
import com.aqa.cc.nodestatus.core.widget.SnapshotWidgetDataSource;
import com.aqa.cc.nodestatus.core.widget.WidgetSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class LatestSnapshotRepository {
    private final SnapshotStore snapshotStore;

    LatestSnapshotRepository(@NonNull Context context) {
        this(new FileSnapshotStore(AppSnapshotFiles.latestSnapshotPath(context)));
    }

    LatestSnapshotRepository(@NonNull SnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    @NonNull
    SiteSnapshots loadSiteSnapshots(@Nullable String siteId) {
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        for (ResourceSnapshot snapshot : snapshotStore.listLatest()) {
            if (belongsToSite(snapshot.getSiteId(), siteId)) {
                snapshots.add(snapshot);
            }
        }
        snapshots.sort(Comparator.comparing(ResourceSnapshot::getCollectedAt).reversed());
        return new SiteSnapshots(snapshots, buildSummaries(snapshots));
    }

    @Nullable
    ResourceSnapshot findLatestForSite(@Nullable String siteId) {
        return loadSiteSnapshots(siteId).latestSnapshot();
    }

    static boolean belongsToSite(@Nullable String snapshotSiteId, @Nullable String requestedSiteId) {
        String resolvedSiteId = normalizeSiteId(requestedSiteId);
        if (resolvedSiteId.equals(snapshotSiteId)) {
            return true;
        }
        return SiteCatalogStore.DEFAULT_SITE_ID.equals(resolvedSiteId)
                && (snapshotSiteId == null || snapshotSiteId.isBlank());
    }

    @NonNull
    private List<WidgetSummary> buildSummaries(@NonNull List<ResourceSnapshot> snapshots) {
        return new SnapshotWidgetDataSource(new InMemorySnapshotStore(snapshots))
                .loadSummaries(Integer.MAX_VALUE);
    }

    @NonNull
    private static String normalizeSiteId(@Nullable String siteId) {
        if (siteId == null || siteId.isBlank()) {
            return SiteCatalogStore.DEFAULT_SITE_ID;
        }
        return siteId;
    }

    static final class SiteSnapshots {
        private static final SiteSnapshots EMPTY = new SiteSnapshots(List.of(), List.of());

        private final List<ResourceSnapshot> snapshots;
        private final List<WidgetSummary> summaries;

        SiteSnapshots(
                @NonNull List<ResourceSnapshot> snapshots,
                @NonNull List<WidgetSummary> summaries
        ) {
            this.snapshots = List.copyOf(snapshots);
            this.summaries = List.copyOf(summaries);
        }

        @NonNull
        static SiteSnapshots empty() {
            return EMPTY;
        }

        @NonNull
        List<WidgetSummary> getSummaries() {
            return summaries;
        }

        @Nullable
        ResourceSnapshot latestSnapshot() {
            if (snapshots.isEmpty()) {
                return null;
            }
            return snapshots.get(0);
        }

        @Nullable
        ResourceSnapshot findSnapshot(@Nullable String scopedResourceId) {
            if (scopedResourceId == null || scopedResourceId.isBlank()) {
                return null;
            }
            for (ResourceSnapshot snapshot : snapshots) {
                if (scopedResourceId.equals(snapshot.getScopedResourceId())
                        || scopedResourceId.equals(snapshot.getResourceId())) {
                    return snapshot;
                }
                if (snapshot.getSiteId().isBlank()
                        && scopedResourceId.endsWith("::" + snapshot.getResourceId())) {
                    return snapshot;
                }
            }
            return null;
        }
    }

    private static final class InMemorySnapshotStore implements SnapshotStore {
        private final List<ResourceSnapshot> snapshots;

        private InMemorySnapshotStore(@NonNull List<ResourceSnapshot> snapshots) {
            this.snapshots = List.copyOf(snapshots);
        }

        @Override
        public void save(@NonNull ResourceSnapshot snapshot) {
            throw new UnsupportedOperationException("Read-only snapshot store.");
        }

        @Override
        public void saveAll(@NonNull List<ResourceSnapshot> snapshots) {
            throw new UnsupportedOperationException("Read-only snapshot store.");
        }

        @Override
        public @Nullable ResourceSnapshot find(@NonNull String resourceId) {
            for (ResourceSnapshot snapshot : snapshots) {
                if (resourceId.equals(snapshot.getScopedResourceId())
                        || resourceId.equals(snapshot.getResourceId())) {
                    return snapshot;
                }
            }
            return null;
        }

        @Override
        public @NonNull List<ResourceSnapshot> listLatest() {
            return snapshots;
        }
    }
}
