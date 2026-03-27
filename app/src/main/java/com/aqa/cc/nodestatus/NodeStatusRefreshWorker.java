package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aqa.cc.nodestatus.core.model.SiteProfile;

public class NodeStatusRefreshWorker extends Worker {
    public NodeStatusRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        ActiveSiteSessionRepository siteSessionRepository = new ActiveSiteSessionRepository(getApplicationContext());
        SiteProfile activeSite = siteSessionRepository.loadActiveSite();
        ProviderSessionConfig config = siteSessionRepository.loadActiveConfig();
        NodeStatusRefreshCoordinator.reconcileRefreshWork(getApplicationContext(), config);
        if (!config.hasRunnableAuth()) {
            return Result.success();
        }

        try {
            NodeStatusRefreshCoordinator.refresh(
                    getApplicationContext(),
                    activeSite,
                    config,
                    NodeStatusRefreshCoordinator.SOURCE_WORKER
            );
            return Result.success();
        } catch (Throwable throwable) {
            String message = throwable.getMessage();
            if (message == null || message.isBlank()) {
                message = throwable.getClass().getSimpleName();
            }
            new NodeStatusRefreshStatusStore(getApplicationContext()).saveFailure(
                    NodeStatusRefreshCoordinator.SOURCE_WORKER,
                    message,
                    java.time.Instant.now().toString(),
                    RefreshDebugLogComposer.formatThrowable(throwable)
            );
            NodeStatusWidgetProvider.refreshAll(getApplicationContext());
            return NodeStatusRefreshFailurePolicy.classify(throwable) == NodeStatusRefreshFailurePolicy.Disposition.RETRY
                    ? Result.retry()
                    : Result.failure();
        }
    }
}
