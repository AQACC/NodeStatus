package com.aqa.cc.nodestatus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;

public class NodeStatusWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_MANUAL_REFRESH = "com.aqa.cc.nodestatus.action.MANUAL_REFRESH_WIDGET";
    private static final int MINI_WIDGET_MAX_WIDTH_DP = 190;
    private static final int MINI_WIDGET_MAX_HEIGHT_DP = 165;
    private static final int COMPACT_WIDGET_MAX_WIDTH_DP = 210;
    private static final int COMPACT_WIDGET_MAX_HEIGHT_DP = 190;

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, NodeStatusWidgetProvider.class);
        int[] widgetIds = manager.getAppWidgetIds(componentName);
        for (int widgetId : widgetIds) {
            updateWidget(context, manager, widgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager appWidgetManager,
            int appWidgetId,
            Bundle newOptions
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateWidget(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_MANUAL_REFRESH.equals(intent.getAction())) {
            ProviderSessionConfig config = new ActiveSiteSessionRepository(context).loadActiveConfig();
            if (config.hasRunnableAuth()) {
                new NodeStatusRefreshStatusStore(context).savePending(
                        NodeStatusRefreshCoordinator.SOURCE_WIDGET,
                        context.getString(R.string.widget_refresh_queued),
                        java.time.Instant.now().toString()
                );
                NodeStatusRefreshCoordinator.enqueueImmediateRefresh(context, config);
                refreshAll(context);
            }
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget_node_status);
        ActiveSiteSessionRepository siteSessionRepository = new ActiveSiteSessionRepository(context);
        String activeSiteId = siteSessionRepository.loadActiveSiteId();
        ResourceSnapshot snapshot = new LatestSnapshotRepository(context).findLatestForSite(activeSiteId);
        NodeStatusRefreshStatusStore.RefreshStatus status = new NodeStatusRefreshStatusStore(context).load();
        LayoutMode layoutMode = resolveLayoutMode(manager.getAppWidgetOptions(widgetId));

        bindWidgetActions(context, views);
        applyLayoutMode(views, layoutMode);
        if (snapshot == null) {
            renderWithoutSnapshot(context, views, status);
        } else {
            renderSnapshot(context, views, snapshot, status);
        }
        manager.updateAppWidget(widgetId, views);
    }

    private static void renderWithoutSnapshot(
            Context context,
            RemoteViews views,
            NodeStatusRefreshStatusStore.RefreshStatus status
    ) {
        views.setViewVisibility(R.id.widgetMiniMetricsSection, View.GONE);
        views.setViewVisibility(R.id.widgetCompactMetricsSection, View.GONE);
        views.setViewVisibility(R.id.widgetMetricsSection, View.GONE);
        views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_empty_title));

        if (status != null && status.isPending()) {
            views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refreshing));
            views.setTextViewText(R.id.widgetMessage, context.getString(R.string.widget_refresh_pending_body));
            views.setViewVisibility(R.id.widgetMessage, View.VISIBLE);
            return;
        }

        if (status != null && !status.isSuccess()) {
            views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refresh_failed));
            views.setTextViewText(
                    R.id.widgetMessage,
                    context.getString(R.string.widget_refresh_failed_body, status.getMessage())
            );
            views.setViewVisibility(R.id.widgetMessage, View.VISIBLE);
            return;
        }

        views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_empty_subtitle));
        views.setTextViewText(R.id.widgetMessage, context.getString(R.string.widget_empty_body));
        views.setViewVisibility(R.id.widgetMessage, View.VISIBLE);
    }

    private static void renderSnapshot(
            Context context,
            RemoteViews views,
            ResourceSnapshot snapshot,
            NodeStatusRefreshStatusStore.RefreshStatus status
    ) {
        views.setTextViewText(R.id.widgetTitle, snapshot.getDisplayName());
        UsageVisual memoryVisual = resolveUsageVisual(
                context,
                snapshot,
                "usage.memory_used_bytes",
                "quota.memory_bytes"
        );
        views.setTextViewText(R.id.widgetMemoryPercent, memoryVisual.percentText);
        views.setTextViewText(R.id.widgetMemoryValue, memoryVisual.detailText);
        views.setProgressBar(R.id.widgetMemoryBar, 100, memoryVisual.percentValue, false);

        UsageVisual diskVisual = resolveUsageVisual(
                context,
                snapshot,
                "usage.disk_used_bytes",
                "quota.disk_bytes"
        );
        views.setTextViewText(R.id.widgetDiskPercent, diskVisual.percentText);
        views.setTextViewText(R.id.widgetDiskValue, diskVisual.detailText);
        views.setProgressBar(R.id.widgetDiskBar, 100, diskVisual.percentValue, false);

        TrafficVisual trafficVisual = resolveTrafficVisual(context, snapshot);
        views.setTextViewText(R.id.widgetTrafficLabel, context.getString(R.string.widget_traffic_focus_label));
        views.setTextViewText(R.id.widgetTrafficValue, trafficVisual.valueText);
        views.setTextViewText(R.id.widgetTrafficDetail, trafficVisual.detailText);
        views.setViewVisibility(R.id.widgetTrafficDetail, trafficVisual.detailText.isEmpty() ? View.GONE : View.VISIBLE);
        views.setTextViewText(R.id.widgetTrafficPercent, trafficVisual.percentText);
        views.setProgressBar(R.id.widgetTrafficBar, 100, trafficVisual.progressPercent, false);
        bindCompactSnapshotViews(context, views, memoryVisual, diskVisual, trafficVisual);
        bindMiniSnapshotViews(views, memoryVisual, diskVisual, trafficVisual);

        if (status != null && status.isPending()) {
            views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refreshing));
            views.setViewVisibility(R.id.widgetMessage, View.GONE);
            return;
        }

        if (status != null && !status.isSuccess()) {
            views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refresh_failed));
            views.setViewVisibility(R.id.widgetMessage, View.GONE);
            return;
        }

        views.setTextViewText(
                R.id.widgetSubtitle,
                context.getString(
                        R.string.widget_updated_prefix,
                        SnapshotTextFormatter.formatInstant(snapshot.getCollectedAt())
                )
        );
        views.setViewVisibility(R.id.widgetMessage, View.GONE);
    }

    private static void bindWidgetActions(Context context, RemoteViews views) {
        Intent launchIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

        Intent refreshIntent = new Intent(context, NodeStatusWidgetProvider.class);
        refreshIntent.setAction(ACTION_MANUAL_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetRefreshButton, refreshPendingIntent);
    }

    private static void bindCompactSnapshotViews(
            Context context,
            RemoteViews views,
            UsageVisual memoryVisual,
            UsageVisual diskVisual,
            TrafficVisual trafficVisual
    ) {
        views.setTextViewText(R.id.widgetCompactTrafficLabel, context.getString(R.string.widget_traffic_focus_label));
        views.setTextViewText(R.id.widgetCompactTrafficValue, trafficVisual.valueText);
        views.setTextViewText(R.id.widgetCompactTrafficDetail, trafficVisual.detailText);
        views.setViewVisibility(R.id.widgetCompactTrafficDetail, trafficVisual.detailText.isEmpty() ? View.GONE : View.VISIBLE);
        views.setTextViewText(R.id.widgetCompactTrafficPercent, trafficVisual.percentText);
        views.setProgressBar(R.id.widgetCompactTrafficBar, 100, trafficVisual.progressPercent, false);

        views.setTextViewText(R.id.widgetCompactMemoryPercent, memoryVisual.percentText);
        views.setTextViewText(R.id.widgetCompactMemoryValue, memoryVisual.detailText);
        views.setProgressBar(R.id.widgetCompactMemoryBar, 100, memoryVisual.percentValue, false);

        views.setTextViewText(R.id.widgetCompactDiskPercent, diskVisual.percentText);
        views.setTextViewText(R.id.widgetCompactDiskValue, diskVisual.detailText);
        views.setProgressBar(R.id.widgetCompactDiskBar, 100, diskVisual.percentValue, false);
    }

    private static void bindMiniSnapshotViews(
            RemoteViews views,
            UsageVisual memoryVisual,
            UsageVisual diskVisual,
            TrafficVisual trafficVisual
    ) {
        views.setTextViewText(R.id.widgetMiniTrafficValue, trafficVisual.valueText);
        views.setTextViewText(R.id.widgetMiniTrafficPercent, trafficVisual.miniPercentText);
        views.setTextViewText(R.id.widgetMiniTrafficTotal, trafficVisual.miniTotalText);
        views.setViewVisibility(R.id.widgetMiniTrafficTotal, trafficVisual.miniTotalText.isEmpty() ? View.GONE : View.VISIBLE);
        views.setProgressBar(R.id.widgetMiniTrafficBar, 100, trafficVisual.progressPercent, false);
        views.setProgressBar(R.id.widgetMiniMemoryBar, 100, memoryVisual.percentValue, false);
        views.setProgressBar(R.id.widgetMiniDiskBar, 100, diskVisual.percentValue, false);
    }

    private static void applyLayoutMode(RemoteViews views, LayoutMode layoutMode) {
        views.setViewVisibility(R.id.widgetMiniMetricsSection, layoutMode == LayoutMode.MINI ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widgetCompactMetricsSection, layoutMode == LayoutMode.COMPACT ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widgetMetricsSection, layoutMode == LayoutMode.REGULAR ? View.VISIBLE : View.GONE);
    }

    private static LayoutMode resolveLayoutMode(Bundle options) {
        if (options == null) {
            return LayoutMode.REGULAR;
        }
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        if ((minWidth > 0 && minWidth < MINI_WIDGET_MAX_WIDTH_DP)
                || (minHeight > 0 && minHeight < MINI_WIDGET_MAX_HEIGHT_DP)) {
            return LayoutMode.MINI;
        }
        if ((minWidth > 0 && minWidth < COMPACT_WIDGET_MAX_WIDTH_DP)
                || (minHeight > 0 && minHeight < COMPACT_WIDGET_MAX_HEIGHT_DP)) {
            return LayoutMode.COMPACT;
        }
        return LayoutMode.REGULAR;
    }

    private static UsageVisual resolveUsageVisual(
            Context context,
            ResourceSnapshot snapshot,
            String usageKey,
            String quotaKey
    ) {
        Integer percent = SnapshotTextFormatter.resolveUsagePercent(snapshot, usageKey, quotaKey);
        return new UsageVisual(
                percent == null ? context.getString(R.string.metric_value_unavailable) : percent + "%",
                buildUsageDetail(context, snapshot, usageKey, quotaKey),
                percent == null ? 0 : percent
        );
    }

    private static String buildUsageDetail(
            Context context,
            ResourceSnapshot snapshot,
            String usageKey,
            String quotaKey
    ) {
        Long usedBytes = SnapshotTextFormatter.findMetricLong(snapshot, usageKey);
        if (usedBytes == null) {
            return context.getString(R.string.metric_value_unavailable);
        }
        Long quotaBytes = SnapshotTextFormatter.findMetricLong(snapshot, quotaKey);
        if (quotaBytes == null || quotaBytes <= 0L) {
            return SnapshotTextFormatter.formatBytes(usedBytes);
        }
        return SnapshotTextFormatter.formatBytes(usedBytes) + " / " + SnapshotTextFormatter.formatBytes(quotaBytes);
    }

    private static TrafficVisual resolveTrafficVisual(Context context, ResourceSnapshot snapshot) {
        String unavailable = context.getString(R.string.metric_value_unavailable);
        Long quotaBytes = SnapshotTextFormatter.findMetricLong(snapshot, "quota.traffic_bytes");
        Long usedBytes = SnapshotTextFormatter.findMetricLong(snapshot, "usage.traffic_total_bytes");
        Long remainingBytes = SnapshotTextFormatter.findMetricLong(snapshot, "usage.traffic_remaining_bytes");

        if (remainingBytes == null && quotaBytes != null && usedBytes != null) {
            remainingBytes = Math.max(0L, quotaBytes - usedBytes);
        }
        if (usedBytes == null && quotaBytes != null && remainingBytes != null) {
            usedBytes = Math.max(0L, quotaBytes - remainingBytes);
        }

        if (remainingBytes != null) {
            Integer remainingPercent = resolvePercent(remainingBytes, quotaBytes);
            return new TrafficVisual(
                    SnapshotTextFormatter.formatBytes(remainingBytes),
                    buildTrafficDetail(context, usedBytes, quotaBytes),
                    buildTrafficPercentText(context, remainingPercent, true),
                    buildMiniTrafficPercentText(context, remainingPercent),
                    buildMiniTrafficTotalText(context, quotaBytes),
                    remainingPercent == null ? 0 : remainingPercent
            );
        }

        if (usedBytes != null) {
            Integer usedPercent = resolvePercent(usedBytes, quotaBytes);
            return new TrafficVisual(
                    SnapshotTextFormatter.formatBytes(usedBytes),
                    buildTrafficDetail(context, usedBytes, quotaBytes),
                    buildTrafficPercentText(context, usedPercent, false),
                    buildMiniTrafficPercentText(context, usedPercent),
                    buildMiniTrafficTotalText(context, quotaBytes),
                    usedPercent == null ? 0 : usedPercent
            );
        }

        if (quotaBytes != null) {
            String quotaText = SnapshotTextFormatter.formatBytes(quotaBytes);
            return new TrafficVisual(
                    quotaText,
                    context.getString(R.string.widget_traffic_quota_only, quotaText),
                    unavailable,
                    unavailable,
                    buildMiniTrafficTotalText(context, quotaBytes),
                    0
            );
        }

        return new TrafficVisual(unavailable, "", unavailable, unavailable, "", 0);
    }

    private static String buildTrafficDetail(Context context, Long usedBytes, Long quotaBytes) {
        if (usedBytes != null && quotaBytes != null && quotaBytes > 0L) {
            return context.getString(
                    R.string.widget_traffic_detail_format,
                    SnapshotTextFormatter.formatBytes(usedBytes),
                    SnapshotTextFormatter.formatBytes(quotaBytes)
            );
        }
        if (quotaBytes != null && quotaBytes > 0L) {
            return context.getString(
                    R.string.widget_traffic_quota_only,
                    SnapshotTextFormatter.formatBytes(quotaBytes)
            );
        }
        if (usedBytes != null) {
            return context.getString(
                    R.string.widget_traffic_used_only,
                    SnapshotTextFormatter.formatBytes(usedBytes)
            );
        }
        return "";
    }

    private static String buildTrafficPercentText(Context context, Integer percent, boolean remaining) {
        if (percent == null) {
            return context.getString(R.string.metric_value_unavailable);
        }
        String percentText = percent + "%";
        if (remaining) {
            return context.getString(R.string.widget_traffic_remaining_percent, percentText);
        }
        return context.getString(R.string.widget_traffic_used_percent, percentText);
    }

    private static String buildMiniTrafficPercentText(Context context, Integer percent) {
        if (percent == null) {
            return context.getString(R.string.metric_value_unavailable);
        }
        return percent + "%";
    }

    private static String buildMiniTrafficTotalText(Context context, Long quotaBytes) {
        if (quotaBytes == null || quotaBytes <= 0L) {
            return "";
        }
        return context.getString(
                R.string.widget_traffic_total_short,
                SnapshotTextFormatter.formatBytes(quotaBytes)
        );
    }

    private static Integer resolvePercent(Long valueBytes, Long quotaBytes) {
        if (valueBytes == null || quotaBytes == null || quotaBytes <= 0L) {
            return null;
        }
        long rounded = Math.round(valueBytes * 100.0 / quotaBytes);
        if (rounded < 0L) {
            return 0;
        }
        return (int) Math.min(100L, rounded);
    }

    private static final class UsageVisual {
        private final String percentText;
        private final String detailText;
        private final int percentValue;

        private UsageVisual(String percentText, String detailText, int percentValue) {
            this.percentText = percentText;
            this.detailText = detailText;
            this.percentValue = percentValue;
        }
    }

    private static final class TrafficVisual {
        private final String valueText;
        private final String detailText;
        private final String percentText;
        private final String miniPercentText;
        private final String miniTotalText;
        private final int progressPercent;

        private TrafficVisual(
                String valueText,
                String detailText,
                String percentText,
                String miniPercentText,
                String miniTotalText,
                int progressPercent
        ) {
            this.valueText = valueText;
            this.detailText = detailText;
            this.percentText = percentText;
            this.miniPercentText = miniPercentText;
            this.miniTotalText = miniTotalText;
            this.progressPercent = progressPercent;
        }
    }

    private enum LayoutMode {
        REGULAR,
        COMPACT,
        MINI
    }

}
