package com.aqa.cc.nodestatus;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NodeStatusNotificationHelper {
    private static final String CHANNEL_ID = "nodestatus_alerts";
    private static final String CHANNEL_NAME = "NodeStatus Alerts";
    private static final String PREFS_NAME = "nodestatus_alert_state";

    private NodeStatusNotificationHelper() {
    }

    public static void maybeNotifyLowTraffic(
            Context context,
            VirtFusionSessionConfig config,
            List<ResourceSnapshot> snapshots
    ) {
        if (!config.isNotificationsEnabled()) {
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ensureChannel(context);

        for (ResourceSnapshot snapshot : snapshots) {
            Integer remainingPercent = computeRemainingTrafficPercent(snapshot);
            if (remainingPercent == null) {
                continue;
            }

            boolean belowThreshold = remainingPercent <= config.getLowTrafficThresholdPercent();
            boolean wasBelowThreshold = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(snapshot.getResourceId(), false);

            if (belowThreshold && !wasBelowThreshold) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setContentTitle(snapshot.getDisplayName())
                        .setContentText(
                                context.getString(
                                        R.string.notification_low_traffic_body,
                                        remainingPercent,
                                        config.getLowTrafficThresholdPercent()
                                )
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

                NotificationManagerCompat.from(context).notify(snapshot.getResourceId().hashCode(), builder.build());
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(snapshot.getResourceId(), belowThreshold)
                    .apply();
        }
    }

    private static Integer computeRemainingTrafficPercent(ResourceSnapshot snapshot) {
        Map<String, Metric> metrics = new HashMap<>();
        for (Metric metric : snapshot.getMetrics()) {
            metrics.put(metric.getKey(), metric);
        }

        Long quota = parseLongMetric(metrics.get("quota.traffic_bytes"));
        Long remaining = parseLongMetric(metrics.get("usage.traffic_remaining_bytes"));
        Long total = parseLongMetric(metrics.get("usage.traffic_total_bytes"));

        if (quota == null || quota <= 0) {
            return null;
        }
        long remainingBytes;
        if (remaining != null) {
            remainingBytes = remaining;
        } else if (total != null) {
            remainingBytes = Math.max(0L, quota - total);
        } else {
            return null;
        }

        return (int) Math.floor((remainingBytes * 100.0) / quota);
    }

    private static Long parseLongMetric(Metric metric) {
        if (metric == null || metric.getValue() == null) {
            return null;
        }
        try {
            return Long.parseLong(metric.getValue().getRaw());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }
}
