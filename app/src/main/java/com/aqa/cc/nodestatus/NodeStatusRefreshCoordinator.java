package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionApiTokenAuth;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionApiTokenClient;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionLocalSessionAuth;
import com.aqa.cc.nodestatus.adapter.virtfusion.VirtFusionSessionClient;
import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.ResourceKind;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class NodeStatusRefreshCoordinator {
    public static final String UNIQUE_WORK_NAME = "nodestatus_periodic_refresh";
    public static final String UNIQUE_ONE_TIME_WORK_NAME = "nodestatus_immediate_refresh";
    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_WORKER = "worker";
    public static final String SOURCE_WIDGET = "widget";

    private NodeStatusRefreshCoordinator() {
    }

    @NonNull
    public static RefreshOutcome refresh(
            @NonNull Context context,
            @NonNull VirtFusionSessionConfig config,
            @NonNull String source
    ) {
        new NodeStatusRefreshStatusStore(context).savePending(
                source,
                "Refresh in progress.",
                Instant.now().toString()
        );

        String tokenProbeSummary = null;
        List<ResourceSnapshot> tokenSnapshots = List.of();
        List<ResourceSnapshot> sessionSnapshots = List.of();

        if (config.hasApiToken()) {
            VirtFusionApiTokenClient apiTokenClient = VirtFusionApiTokenClient.create(
                    new VirtFusionApiTokenAuth(
                            config.getApiBaseUrl(),
                            config.getApiToken(),
                            config.getUserAgent(),
                            config.isAllowInsecureTls()
                    )
            );
            tokenProbeSummary = apiTokenClient.probeConnection();
            tokenSnapshots = apiTokenClient.fetchSnapshots(8, Instant.now());
        }

        if (config.hasCompatibilitySession()) {
            VirtFusionSessionClient sessionClient = VirtFusionSessionClient.create(
                    new VirtFusionLocalSessionAuth(
                            config.getBaseUrl(),
                            config.getCookieHeader(),
                            config.getXsrfHeader(),
                            config.getUserAgent(),
                            config.isAllowInsecureTls()
                    )
            );
            sessionSnapshots = sessionClient.fetchSnapshots(8, Instant.now());
        }

        List<ResourceSnapshot> snapshotsToStore = mergeSnapshots(tokenSnapshots, sessionSnapshots);
        new FileSnapshotStore(AppSnapshotFiles.latestSnapshotPath(context)).saveAll(snapshotsToStore);
        new FileSnapshotHistoryStore(AppSnapshotFiles.historySnapshotPath(context), 240).appendAll(snapshotsToStore);

        RefreshOutcome outcome = new RefreshOutcome(
                snapshotsToStore.size(),
                tokenProbeSummary,
                !tokenSnapshots.isEmpty(),
                !sessionSnapshots.isEmpty()
        );
        new NodeStatusRefreshStatusStore(context).saveSuccess(
                source,
                outcome.toStatusMessage(),
                Instant.now().toString(),
                outcome.usedToken(),
                outcome.usedCompatibilitySession()
        );
        NodeStatusNotificationHelper.maybeNotifyLowTraffic(context, config, snapshotsToStore);
        NodeStatusWidgetProvider.refreshAll(context.getApplicationContext());
        schedulePeriodicRefresh(context, config);
        return outcome;
    }

    public static void schedulePeriodicRefresh(@NonNull Context context, @NonNull VirtFusionSessionConfig config) {
        if (!config.hasApiToken() && !config.hasCompatibilitySession()) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                NodeStatusRefreshWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
        );
    }

    public static void enqueueImmediateRefresh(@NonNull Context context, @NonNull VirtFusionSessionConfig config) {
        if (!config.hasApiToken() && !config.hasCompatibilitySession()) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(NodeStatusRefreshWorker.class)
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }

    @NonNull
    private static List<ResourceSnapshot> mergeSnapshots(
            @NonNull List<ResourceSnapshot> tokenSnapshots,
            @NonNull List<ResourceSnapshot> sessionSnapshots
    ) {
        if (tokenSnapshots.isEmpty()) {
            return sessionSnapshots;
        }
        if (sessionSnapshots.isEmpty()) {
            return tokenSnapshots;
        }

        Map<String, ResourceSnapshot> sessionById = new LinkedHashMap<>();
        for (ResourceSnapshot snapshot : sessionSnapshots) {
            sessionById.put(snapshot.getResourceId(), snapshot);
        }

        List<ResourceSnapshot> merged = new ArrayList<>();
        for (ResourceSnapshot tokenSnapshot : tokenSnapshots) {
            ResourceSnapshot sessionSnapshot = sessionById.remove(tokenSnapshot.getResourceId());
            if (sessionSnapshot == null) {
                merged.add(tokenSnapshot);
            } else {
                merged.add(mergeSnapshot(tokenSnapshot, sessionSnapshot));
            }
        }
        merged.addAll(sessionById.values());
        return merged;
    }

    @NonNull
    private static ResourceSnapshot mergeSnapshot(
            @NonNull ResourceSnapshot tokenSnapshot,
            @NonNull ResourceSnapshot sessionSnapshot
    ) {
        Map<String, Metric> mergedMetrics = new LinkedHashMap<>();
        for (Metric metric : tokenSnapshot.getMetrics()) {
            mergedMetrics.put(metric.getKey(), metric);
        }
        for (Metric metric : sessionSnapshot.getMetrics()) {
            mergedMetrics.put(metric.getKey(), metric);
        }

        return new ResourceSnapshot(
                tokenSnapshot.getResourceId(),
                sessionSnapshot.getDisplayName(),
                ProviderFamily.VIRT_FUSION,
                ResourceKind.VIRTUAL_MACHINE,
                sessionSnapshot.getCollectedAt(),
                new ArrayList<>(mergedMetrics.values())
        );
    }

    public static final class RefreshOutcome {
        private final int snapshotCount;
        private final String tokenProbeSummary;
        private final boolean usedToken;
        private final boolean usedCompatibilitySession;

        public RefreshOutcome(int snapshotCount, String tokenProbeSummary, boolean usedToken, boolean usedCompatibilitySession) {
            this.snapshotCount = snapshotCount;
            this.tokenProbeSummary = tokenProbeSummary;
            this.usedToken = usedToken;
            this.usedCompatibilitySession = usedCompatibilitySession;
        }

        public int getSnapshotCount() {
            return snapshotCount;
        }

        public String getTokenProbeSummary() {
            return tokenProbeSummary;
        }

        public boolean usedToken() {
            return usedToken;
        }

        public boolean usedCompatibilitySession() {
            return usedCompatibilitySession;
        }

        public String toStatusMessage() {
            if (usedToken && usedCompatibilitySession) {
                return "Saved " + snapshotCount + " snapshot(s) using API token plus compatibility session.";
            }
            if (usedToken) {
                return "Saved " + snapshotCount + " snapshot(s) using API token.";
            }
            return "Saved " + snapshotCount + " snapshot(s) using compatibility session.";
        }
    }
}
