package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.core.model.Freshness;
import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.MetricValue;
import com.aqa.cc.nodestatus.core.model.MetricValueType;
import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.ResourceKind;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

public class NodeStatusRefreshCoordinatorTest {
    @Test
    public void refresh_session_only_records_side_effects() {
        FakeDependencies dependencies = new FakeDependencies();
        dependencies.sessionSnapshots = List.of(snapshot("server-a", "Alpha", "state.power", "running"));

        NodeStatusRefreshCoordinator.RefreshOutcome outcome = NodeStatusRefreshCoordinator.refresh(
                new SiteProfile("site-a", "Alpha site", "https://panel.example.com", ProviderFamily.VIRT_FUSION),
                runnableSessionConfig(),
                new FakeProviderDescriptorRegistry(dependencies),
                NodeStatusRefreshCoordinator.SOURCE_MANUAL,
                dependencies
        );

        Assert.assertEquals(1, outcome.getSnapshotCount());
        Assert.assertTrue(outcome.usedCompatibilitySession());
        Assert.assertEquals(1, dependencies.savedLatestSnapshots.size());
        Assert.assertEquals("Alpha", dependencies.savedLatestSnapshots.get(0).getDisplayName());
        Assert.assertEquals(1, dependencies.savedLatestSnapshots.get(0).getMetrics().size());
        Assert.assertEquals("site-a", dependencies.savedLatestSnapshots.get(0).getSiteId());
        Assert.assertTrue(dependencies.pendingSaved);
        Assert.assertTrue(dependencies.successSaved);
        Assert.assertTrue(dependencies.widgetsRefreshed);
        Assert.assertTrue(dependencies.lowTrafficNotified);
        Assert.assertTrue(dependencies.reconcileCalled);
    }

    @Test
    public void refresh_session_only_fetches_live_snapshots() {
        FakeDependencies dependencies = new FakeDependencies();
        dependencies.sessionSnapshots = List.of(snapshot("server-a", "Alpha", "state.power", "running"));

        NodeStatusRefreshCoordinator.RefreshOutcome outcome = NodeStatusRefreshCoordinator.refresh(
                new SiteProfile("site-a", "Alpha site", "https://panel.example.com", ProviderFamily.VIRT_FUSION),
                runnableSessionConfig(),
                new FakeProviderDescriptorRegistry(dependencies),
                NodeStatusRefreshCoordinator.SOURCE_MANUAL,
                dependencies
        );

        Assert.assertTrue(outcome.usedCompatibilitySession());
        Assert.assertTrue(dependencies.fetchCalled);
        Assert.assertEquals(1, dependencies.savedLatestSnapshots.size());
    }

    @Test(expected = IllegalStateException.class)
    public void refresh_rejects_non_runnable_auth_before_side_effects() {
        FakeDependencies dependencies = new FakeDependencies();

        try {
            NodeStatusRefreshCoordinator.refresh(
                    new SiteProfile("site-a", "Alpha site", "https://panel.example.com", ProviderFamily.VIRT_FUSION),
                    emptyConfig(),
                    new FakeProviderDescriptorRegistry(dependencies),
                    NodeStatusRefreshCoordinator.SOURCE_MANUAL,
                    dependencies
            );
        } finally {
            Assert.assertFalse(dependencies.pendingSaved);
            Assert.assertFalse(dependencies.successSaved);
        }
    }

    private ProviderSessionConfig runnableSessionConfig() {
        return new ProviderSessionConfig(
                "https://panel.example.com",
                "",
                "cookie=value",
                "xsrf-value",
                "agent/1.0",
                false,
                true,
                20
        );
    }

    private ProviderSessionConfig emptyConfig() {
        return new ProviderSessionConfig(
                "",
                "",
                "",
                "",
                "agent/1.0",
                false,
                false,
                20
        );
    }

    private ResourceSnapshot snapshot(String resourceId, String displayName, String metricKey, String metricValue) {
        Instant instant = Instant.parse("2026-03-26T01:00:00Z");
        return new ResourceSnapshot(
                resourceId,
                displayName,
                ProviderFamily.VIRT_FUSION,
                ResourceKind.VIRTUAL_MACHINE,
                instant,
                List.of(
                        new Metric(
                                metricKey,
                                metricKey,
                                new MetricValue(metricValue, MetricValueType.TEXT),
                                null,
                                true,
                                Freshness.CURRENT,
                                "test",
                                instant
                        )
                ),
                "",
                ""
        );
    }

    private static final class FakeDependencies implements NodeStatusRefreshCoordinator.RefreshDependencies {
        private final Instant now = Instant.parse("2026-03-26T01:00:00Z");
        private List<ResourceSnapshot> sessionSnapshots = List.of();
        private List<ResourceSnapshot> savedLatestSnapshots = List.of();
        private boolean fetchCalled;
        private boolean pendingSaved;
        private boolean successSaved;
        private boolean lowTrafficNotified;
        private boolean widgetsRefreshed;
        private boolean reconcileCalled;

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public void savePending(String source, String message, String updatedAt) {
            pendingSaved = true;
        }

        @Override
        public void saveSuccess(String source, String message, String updatedAt, boolean usedSession) {
            successSaved = true;
        }

        @Override
        public void saveLatestSnapshots(List<ResourceSnapshot> snapshots) {
            savedLatestSnapshots = snapshots;
        }

        @Override
        public void appendHistorySnapshots(List<ResourceSnapshot> snapshots) {
            // no-op for test
        }

        @Override
        public void notifyLowTraffic(ProviderRefreshConfig config, List<ResourceSnapshot> snapshots) {
            lowTrafficNotified = true;
        }

        @Override
        public void refreshWidgets() {
            widgetsRefreshed = true;
        }

        @Override
        public void reconcileRefreshWork(ProviderRefreshConfig config) {
            reconcileCalled = true;
        }
    }

    private static final class FakeProviderDescriptorRegistry implements ProviderDescriptorRegistry {
        private final FakeDependencies dependencies;

        private FakeProviderDescriptorRegistry(FakeDependencies dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public ProviderDescriptor getDescriptor(ProviderFamily providerFamily) {
            return ProviderDescriptor.refreshable(
                    ProviderFamily.VIRT_FUSION,
                    0,
                    0,
                    new ExampleProviderSettingsBinder(),
                    new ProviderRefreshAdapter() {
                        @Override
                        public List<ResourceSnapshot> fetchSnapshots(
                                SiteProfile siteProfile,
                                ProviderRefreshConfig config,
                                Instant collectedAt
                        ) {
                            dependencies.fetchCalled = true;
                            return dependencies.sessionSnapshots;
                        }
                    }
            );
        }

        @Override
        public List<ProviderDescriptor> allDescriptors() {
            return List.of(getDescriptor(ProviderFamily.VIRT_FUSION));
        }
    }
}
