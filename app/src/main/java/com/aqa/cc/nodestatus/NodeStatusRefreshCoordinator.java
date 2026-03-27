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

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.model.SiteProfile;

import java.time.Instant;
import java.util.List;
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
            @NonNull ProviderRefreshConfig config,
            @NonNull String source
    ) {
        ActiveSiteSessionRepository siteSessionRepository = new ActiveSiteSessionRepository(context.getApplicationContext());
        return refresh(context, siteSessionRepository.loadActiveSite(), config, source);
    }

    @NonNull
    public static RefreshOutcome refresh(
            @NonNull Context context,
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderRefreshConfig config,
            @NonNull String source
    ) {
        return new NodeStatusRefreshService(
                new AppProviderDescriptorRegistry(),
                new AppRefreshDependencies(context)
        ).refresh(siteProfile, config, source);
    }

    @NonNull
    static RefreshOutcome refresh(
            @NonNull ProviderRefreshConfig config,
            @NonNull String source,
            @NonNull RefreshDependencies dependencies
    ) {
        return refresh(
                defaultSiteProfile(),
                config,
                new AppProviderDescriptorRegistry(),
                source,
                dependencies
        );
    }

    @NonNull
    static RefreshOutcome refresh(
            @NonNull SiteProfile siteProfile,
            @NonNull ProviderRefreshConfig config,
            @NonNull ProviderDescriptorRegistry descriptorRegistry,
            @NonNull String source,
            @NonNull RefreshDependencies dependencies
    ) {
        return new NodeStatusRefreshService(
                descriptorRegistry,
                dependencies
        ).refresh(siteProfile, config, source);
    }

    public static void reconcileRefreshWork(@NonNull Context context, @NonNull ProviderRefreshConfig config) {
        if (config.hasRunnableAuth()) {
            schedulePeriodicRefresh(context, config);
            return;
        }
        cancelRefreshWork(context);
    }

    public static void schedulePeriodicRefresh(@NonNull Context context, @NonNull ProviderRefreshConfig config) {
        if (!config.hasRunnableAuth()) {
            cancelRefreshWork(context);
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

    public static void cancelRefreshWork(@NonNull Context context) {
        WorkManager manager = WorkManager.getInstance(context.getApplicationContext());
        manager.cancelUniqueWork(UNIQUE_ONE_TIME_WORK_NAME);
        manager.cancelUniqueWork(UNIQUE_WORK_NAME);
    }

    public static void enqueueImmediateRefresh(@NonNull Context context, @NonNull ProviderRefreshConfig config) {
        if (!config.hasRunnableAuth()) {
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
    private static SiteProfile defaultSiteProfile() {
        return new SiteProfile(
                SiteCatalogStore.DEFAULT_SITE_ID,
                "VirtFusion",
                "",
                com.aqa.cc.nodestatus.core.model.ProviderFamily.VIRT_FUSION
        );
    }

    public static final class RefreshOutcome {
        private final int snapshotCount;

        public RefreshOutcome(int snapshotCount) {
            this.snapshotCount = snapshotCount;
        }

        public int getSnapshotCount() {
            return snapshotCount;
        }

        public boolean usedCompatibilitySession() {
            return true;
        }

        public String toStatusMessage() {
            return snapshotCount == 1
                    ? "Saved 1 snapshot using compatibility session."
                    : "Saved " + snapshotCount + " snapshots using compatibility session.";
        }
    }

    interface RefreshDependencies {
        @NonNull Instant now();

        void savePending(@NonNull String source, @NonNull String message, @NonNull String updatedAt);

        void saveSuccess(
                @NonNull String source,
                @NonNull String message,
                @NonNull String updatedAt,
                boolean usedSession
        );

        default void saveLatestSnapshots(@NonNull SiteProfile siteProfile, @NonNull List<ResourceSnapshot> snapshots) {
            saveLatestSnapshots(snapshots);
        }

        default void saveLatestSnapshots(@NonNull List<ResourceSnapshot> snapshots) {
        }

        void appendHistorySnapshots(@NonNull List<ResourceSnapshot> snapshots);

        void notifyLowTraffic(@NonNull ProviderRefreshConfig config, @NonNull List<ResourceSnapshot> snapshots);

        void refreshWidgets();

        void reconcileRefreshWork(@NonNull ProviderRefreshConfig config);
    }
}
