package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.core.model.Freshness;
import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.MetricValue;
import com.aqa.cc.nodestatus.core.model.MetricValueType;
import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.ResourceKind;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.SnapshotStore;
import com.aqa.cc.nodestatus.core.widget.WidgetSummary;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

public class LatestSnapshotRepositoryTest {
    @Test
    public void loadSiteSnapshots_filters_to_requested_site_and_exposes_snapshots() {
        LatestSnapshotRepository repository = new LatestSnapshotRepository(new FakeSnapshotStore(
                List.of(
                        snapshot("server-a", "Alpha", "site-a", "Alpha site", "2026-03-26T02:00:00Z"),
                        snapshot("server-b", "Beta", "site-b", "Beta site", "2026-03-26T01:00:00Z")
                )
        ));

        LatestSnapshotRepository.SiteSnapshots siteSnapshots = repository.loadSiteSnapshots("site-a");

        List<WidgetSummary> summaries = siteSnapshots.getSummaries();
        Assert.assertEquals(1, summaries.size());
        Assert.assertEquals("site-a::server-a", summaries.get(0).getScopedResourceId());
        Assert.assertEquals("Alpha", summaries.get(0).getDisplayName());
        Assert.assertEquals("site-a::server-a", siteSnapshots.findSnapshot("site-a::server-a").getScopedResourceId());
    }

    @Test
    public void loadSiteSnapshots_treats_blank_snapshot_site_as_default_site() {
        LatestSnapshotRepository repository = new LatestSnapshotRepository(new FakeSnapshotStore(
                List.of(snapshot("server-a", "Alpha", "", "", "2026-03-26T02:00:00Z"))
        ));

        LatestSnapshotRepository.SiteSnapshots siteSnapshots =
                repository.loadSiteSnapshots(SiteCatalogStore.DEFAULT_SITE_ID);

        Assert.assertEquals(1, siteSnapshots.getSummaries().size());
        Assert.assertNotNull(siteSnapshots.findSnapshot("site-default::server-a"));
    }

    @Test
    public void findLatestForSite_returns_most_recent_snapshot() {
        LatestSnapshotRepository repository = new LatestSnapshotRepository(new FakeSnapshotStore(
                List.of(
                        snapshot("server-a", "Alpha older", "site-a", "Alpha site", "2026-03-26T01:00:00Z"),
                        snapshot("server-a", "Alpha newer", "site-a", "Alpha site", "2026-03-26T03:00:00Z")
                )
        ));

        ResourceSnapshot latest = repository.findLatestForSite("site-a");

        Assert.assertNotNull(latest);
        Assert.assertEquals("Alpha newer", latest.getDisplayName());
    }

    private ResourceSnapshot snapshot(
            String resourceId,
            String displayName,
            String siteId,
            String siteDisplayName,
            String collectedAt
    ) {
        Instant instant = Instant.parse(collectedAt);
        return new ResourceSnapshot(
                resourceId,
                displayName,
                ProviderFamily.VIRT_FUSION,
                ResourceKind.VIRTUAL_MACHINE,
                instant,
                List.of(new Metric(
                        "state.power",
                        "Power state",
                        new MetricValue("running", MetricValueType.TEXT),
                        null,
                        true,
                        Freshness.CURRENT,
                        "test",
                        instant
                )),
                siteId,
                siteDisplayName
        );
    }

    private static final class FakeSnapshotStore implements SnapshotStore {
        private final List<ResourceSnapshot> snapshots;

        private FakeSnapshotStore(List<ResourceSnapshot> snapshots) {
            this.snapshots = snapshots;
        }

        @Override
        public void save(ResourceSnapshot snapshot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void saveAll(List<ResourceSnapshot> snapshots) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceSnapshot find(String resourceId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ResourceSnapshot> listLatest() {
            return snapshots;
        }
    }
}
