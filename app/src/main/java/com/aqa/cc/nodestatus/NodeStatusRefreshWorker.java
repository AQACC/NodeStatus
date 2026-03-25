package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NodeStatusRefreshWorker extends Worker {
    public NodeStatusRefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        VirtFusionSessionConfig config = new VirtFusionSessionConfigStore(getApplicationContext()).load();
        if (config.getBaseUrl().isEmpty()) {
            return Result.success();
        }
        if (!config.hasApiToken() && !config.hasCompatibilitySession()) {
            return Result.success();
        }

        try {
            NodeStatusRefreshCoordinator.refresh(
                    getApplicationContext(),
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
                    java.time.Instant.now().toString()
            );
            NodeStatusWidgetProvider.refreshAll(getApplicationContext());
            return Result.retry();
        }
    }
}
