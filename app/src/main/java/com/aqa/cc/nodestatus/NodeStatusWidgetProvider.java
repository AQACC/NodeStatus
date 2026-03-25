package com.aqa.cc.nodestatus;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore;
import com.aqa.cc.nodestatus.core.widget.SnapshotWidgetDataSource;
import com.aqa.cc.nodestatus.core.widget.WidgetMetric;
import com.aqa.cc.nodestatus.core.widget.WidgetSummary;

import java.util.List;

public class NodeStatusWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_MANUAL_REFRESH = "com.aqa.cc.nodestatus.action.MANUAL_REFRESH_WIDGET";

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
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_MANUAL_REFRESH.equals(intent.getAction())) {
            VirtFusionSessionConfig config = new VirtFusionSessionConfigStore(context).load();
            if (config.hasApiToken() || config.hasCompatibilitySession()) {
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
        List<WidgetSummary> summaries = new SnapshotWidgetDataSource(
                new FileSnapshotStore(AppSnapshotFiles.latestSnapshotPath(context))
        ).loadSummaries(1);
        NodeStatusRefreshStatusStore.RefreshStatus status =
                new NodeStatusRefreshStatusStore(context).load();

        if (summaries.isEmpty()) {
            views.setTextViewText(R.id.widgetTitle, context.getString(R.string.widget_empty_title));
            if (status != null && status.isPending()) {
                views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refreshing));
                views.setTextViewText(R.id.widgetMetrics, context.getString(R.string.widget_refresh_pending_body));
                views.setTextViewText(
                        R.id.widgetUpdated,
                        context.getString(R.string.widget_updated_prefix, status.getUpdatedAt())
                );
            } else if (status != null && !status.isSuccess()) {
                views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refresh_failed));
                views.setTextViewText(
                        R.id.widgetMetrics,
                        context.getString(R.string.widget_refresh_failed_body, status.getMessage())
                );
                views.setTextViewText(
                        R.id.widgetUpdated,
                        context.getString(R.string.widget_updated_prefix, status.getUpdatedAt())
                );
            } else {
                views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_empty_subtitle));
                views.setTextViewText(R.id.widgetMetrics, context.getString(R.string.widget_empty_body));
                views.setTextViewText(R.id.widgetUpdated, "");
            }
        } else {
            WidgetSummary summary = summaries.get(0);
            views.setTextViewText(R.id.widgetTitle, summary.getDisplayName());

            if (status != null && status.isPending()) {
                views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refreshing));
                views.setTextViewText(R.id.widgetMetrics, context.getString(R.string.widget_refresh_pending_body));
                views.setTextViewText(
                        R.id.widgetUpdated,
                        context.getString(R.string.widget_updated_prefix, status.getUpdatedAt())
                );
            } else if (status != null && !status.isSuccess()) {
                views.setTextViewText(R.id.widgetSubtitle, context.getString(R.string.widget_refresh_failed));
                views.setTextViewText(
                        R.id.widgetMetrics,
                        context.getString(R.string.widget_refresh_failed_body, status.getMessage())
                );
                views.setTextViewText(
                        R.id.widgetUpdated,
                        context.getString(R.string.widget_updated_prefix, status.getUpdatedAt())
                );
            } else {
                views.setTextViewText(
                        R.id.widgetSubtitle,
                        summary.getHighlights().isEmpty()
                                ? context.getString(R.string.widget_empty_subtitle)
                                : summary.getHighlights().get(0).getLabel() + ": " + summary.getHighlights().get(0).getValueText()
                );
                views.setTextViewText(R.id.widgetMetrics, renderHighlights(summary.getHighlights()));
                views.setTextViewText(
                        R.id.widgetUpdated,
                        context.getString(R.string.widget_updated_prefix, summary.getCollectedAt().toString())
                );
            }
        }

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

        manager.updateAppWidget(widgetId, views);
    }

    private static String renderHighlights(List<WidgetMetric> highlights) {
        StringBuilder builder = new StringBuilder();
        for (int index = 1; index < highlights.size(); index++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            WidgetMetric metric = highlights.get(index);
            builder.append(metric.getLabel())
                    .append(": ")
                    .append(metric.getValueText());
        }
        return builder.toString();
    }
}
