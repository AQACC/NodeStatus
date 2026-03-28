package com.aqa.cc.nodestatus;

import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.widget.WidgetMetric;
import com.aqa.cc.nodestatus.core.widget.WidgetSummary;

final class DashboardContentRenderer {
    private final AppCompatActivity activity;
    private final TextView snapshotStatusValue;
    private final TextView snapshotUpdatedValue;
    private final TextView snapshotSourceValue;
    private final TextView snapshotMetaValue;
    private final TextView trafficTrendValue;
    private final TextView trafficTrendDelta;
    private final TextView memoryTrendValue;
    private final TextView memoryTrendDelta;
    private final TextView metricPowerValue;
    private final TextView metricPowerMeta;
    private final TextView metricTrafficValue;
    private final TextView metricTrafficMeta;
    private final TextView metricCpuValue;
    private final TextView metricCpuMeta;
    private final TextView metricMemoryValue;
    private final TextView metricMemoryMeta;
    private final ProgressBar metricTrafficBar;
    private final ProgressBar metricCpuBar;
    private final ProgressBar metricMemoryBar;
    private final TrendLineView trafficTrendView;
    private final TrendLineView memoryTrendView;
    private final Button openHistoryDetailButton;

    DashboardContentRenderer(AppCompatActivity activity) {
        this.activity = activity;
        this.snapshotStatusValue = activity.findViewById(R.id.snapshotStatusValue);
        this.snapshotUpdatedValue = activity.findViewById(R.id.snapshotUpdatedValue);
        this.snapshotSourceValue = activity.findViewById(R.id.snapshotSourceValue);
        this.snapshotMetaValue = activity.findViewById(R.id.snapshotMetaValue);
        this.trafficTrendValue = activity.findViewById(R.id.trafficTrendValue);
        this.trafficTrendDelta = activity.findViewById(R.id.trafficTrendDelta);
        this.memoryTrendValue = activity.findViewById(R.id.memoryTrendValue);
        this.memoryTrendDelta = activity.findViewById(R.id.memoryTrendDelta);
        this.metricPowerValue = activity.findViewById(R.id.metricPowerValue);
        this.metricPowerMeta = activity.findViewById(R.id.metricPowerMeta);
        this.metricTrafficValue = activity.findViewById(R.id.metricTrafficValue);
        this.metricTrafficMeta = activity.findViewById(R.id.metricTrafficMeta);
        this.metricCpuValue = activity.findViewById(R.id.metricCpuValue);
        this.metricCpuMeta = activity.findViewById(R.id.metricCpuMeta);
        this.metricMemoryValue = activity.findViewById(R.id.metricMemoryValue);
        this.metricMemoryMeta = activity.findViewById(R.id.metricMemoryMeta);
        this.metricTrafficBar = activity.findViewById(R.id.metricTrafficBar);
        this.metricCpuBar = activity.findViewById(R.id.metricCpuBar);
        this.metricMemoryBar = activity.findViewById(R.id.metricMemoryBar);
        this.trafficTrendView = activity.findViewById(R.id.trafficTrendView);
        this.memoryTrendView = activity.findViewById(R.id.memoryTrendView);
        this.openHistoryDetailButton = activity.findViewById(R.id.openHistoryDetailButton);
    }

    void renderEmptyState() {
        openHistoryDetailButton.setEnabled(false);
        snapshotStatusValue.setText(activity.getString(R.string.snapshot_empty_status));
        snapshotUpdatedValue.setText(
                activity.getString(
                        R.string.snapshot_store_path_prefix,
                        AppSnapshotFiles.latestSnapshotPath(activity).toString()
                )
        );
        snapshotSourceValue.setText(R.string.snapshot_source_empty);
        snapshotMetaValue.setText(activity.getString(R.string.snapshot_empty_body));
        renderMetricPlaceholders();
        renderTrendCards(null);
    }

    void renderSelectedSummary(WidgetSummary summary, @Nullable ResourceSnapshot snapshot) {
        openHistoryDetailButton.setEnabled(true);
        snapshotStatusValue.setText(summary.getDisplayName());
        snapshotUpdatedValue.setText(
                activity.getString(
                        R.string.snapshot_updated_prefix,
                        SnapshotTextFormatter.formatInstant(summary.getCollectedAt())
                )
        );
        snapshotSourceValue.setText(resolveSnapshotSourceLabel());
        snapshotMetaValue.setText(buildSnapshotMeta(summary, snapshot));
        renderMetricOverview(summary, snapshot);
        renderTrendCards(summary.getScopedResourceId());
    }

    void renderBackgroundStatus() {
        // Refresh status is now rendered in the settings/debug area instead of the dashboard.
    }

    private CharSequence resolveSnapshotSourceLabel() {
        NodeStatusRefreshStatusStore.RefreshStatus status =
                new NodeStatusRefreshStatusStore(activity).load();
        if (status == null) {
            return activity.getString(R.string.snapshot_source_empty);
        }
        if (status.isUsedCompatibilitySession()) {
            return activity.getString(R.string.snapshot_source_session);
        }
        return activity.getString(R.string.snapshot_source_empty);
    }

    private CharSequence buildSnapshotMeta(WidgetSummary summary, @Nullable ResourceSnapshot snapshot) {
        if (snapshot == null) {
            return resolveWorkspaceLabel(summary);
        }
        StringBuilder builder = new StringBuilder();
        appendSnapshotMetaSegment(
                builder,
                activity.getString(R.string.snapshot_signal_power),
                SnapshotTextFormatter.formatMetricValue(snapshot, "state.power", "")
        );
        appendSnapshotMetaSegment(
                builder,
                activity.getString(R.string.snapshot_signal_ipv4),
                resolveIpv4(snapshot)
        );
        if (builder.length() == 0) {
            return resolveWorkspaceLabel(summary);
        }
        return builder;
    }

    private void renderMetricOverview(WidgetSummary summary, @Nullable ResourceSnapshot snapshot) {
        if (snapshot == null) {
            metricPowerValue.setText(findHighlightValue(summary, "state.power"));
            metricPowerMeta.setText(resolveWorkspaceLabel(summary));
            metricTrafficValue.setText(resolveTrafficFallback(summary));
            metricTrafficMeta.setText(emptyMetricLabel());
            metricCpuValue.setText(findHighlightValue(summary, "state.cpu"));
            metricCpuMeta.setText(emptyMetricLabel());
            metricMemoryValue.setText(findHighlightValue(summary, "usage.memory_used_bytes"));
            metricMemoryMeta.setText(emptyMetricLabel());
            setUsageProgress(metricTrafficBar, null);
            setUsageProgress(metricCpuBar, null);
            setUsageProgress(metricMemoryBar, null);
            return;
        }

        metricPowerValue.setText(resolveMetricValue(summary, snapshot, "state.power"));
        metricPowerMeta.setText(resolveWorkspaceLabel(summary));

        metricTrafficValue.setText(resolveTrafficValue(summary, snapshot));
        metricTrafficMeta.setText(resolveTrafficMeta(snapshot));
        setUsageProgress(metricTrafficBar, resolveTrafficPercent(snapshot));

        metricCpuValue.setText(resolveMetricValue(summary, snapshot, "state.cpu"));
        metricCpuMeta.setText(resolveCpuMeta(summary, snapshot));
        setUsageProgress(
                metricCpuBar,
                resolvePercentMetric(snapshot, "state.cpu")
        );

        metricMemoryValue.setText(resolveUsageValue(summary, snapshot, "usage.memory_used_bytes", "quota.memory_bytes"));
        metricMemoryMeta.setText(resolveMemoryMeta(snapshot));
        setUsageProgress(
                metricMemoryBar,
                SnapshotTextFormatter.resolveUsagePercent(snapshot, "usage.memory_used_bytes", "quota.memory_bytes")
        );
    }

    private String findHighlightValue(WidgetSummary summary, String key) {
        for (WidgetMetric metric : summary.getHighlights()) {
            if (key.equals(metric.getKey())) {
                return metric.getValueText();
            }
        }
        return emptyMetricLabel();
    }

    private void renderTrendCards(@Nullable String resourceId) {
        if (resourceId == null) {
            trafficTrendValue.setText(R.string.trend_empty_value);
            trafficTrendDelta.setText(R.string.trend_empty_delta);
            memoryTrendValue.setText(R.string.trend_empty_value);
            memoryTrendDelta.setText(R.string.trend_empty_delta);
            trafficTrendView.setSeries(new float[0]);
            memoryTrendView.setSeries(new float[0]);
            return;
        }

        SnapshotTrendAnalyzer.TrendBundle bundle = new SnapshotTrendAnalyzer(activity).load(resourceId);
        SnapshotTrendAnalyzer.TrendCardData traffic = bundle.getTrafficTrend();
        SnapshotTrendAnalyzer.TrendCardData memory = bundle.getMemoryTrend();

        trafficTrendValue.setText(activity.getString(R.string.trend_traffic_label, traffic.getValueLabel()));
        trafficTrendDelta.setText(traffic.getChangeLabel());
        trafficTrendView.setSeries(traffic.getPoints());

        memoryTrendValue.setText(activity.getString(R.string.trend_memory_label, memory.getValueLabel()));
        memoryTrendDelta.setText(memory.getChangeLabel());
        memoryTrendView.setSeries(memory.getPoints());
    }

    private void renderMetricPlaceholders() {
        String empty = emptyMetricLabel();
        metricPowerValue.setText(empty);
        metricPowerMeta.setText(empty);
        metricTrafficValue.setText(empty);
        metricTrafficMeta.setText(empty);
        metricCpuValue.setText(empty);
        metricCpuMeta.setText(empty);
        metricMemoryValue.setText(empty);
        metricMemoryMeta.setText(empty);
        setUsageProgress(metricTrafficBar, null);
        setUsageProgress(metricCpuBar, null);
        setUsageProgress(metricMemoryBar, null);
    }

    private String resolveWorkspaceLabel(WidgetSummary summary) {
        String siteDisplayName = summary.getSiteDisplayName();
        if (siteDisplayName == null || siteDisplayName.isBlank()) {
            return activity.getString(R.string.snapshot_workspace_default);
        }
        return siteDisplayName;
    }

    private String resolveIpv4(ResourceSnapshot snapshot) {
        String primaryIpv4 = SnapshotTextFormatter.findMetricText(snapshot, "meta.primary_ipv4", "");
        if (!primaryIpv4.isBlank()) {
            return primaryIpv4;
        }
        return SnapshotTextFormatter.findMetricText(snapshot, "ipv4", emptyMetricLabel());
    }

    private String resolveMetricValue(WidgetSummary summary, ResourceSnapshot snapshot, String key) {
        String formatted = SnapshotTextFormatter.formatMetricValue(snapshot, key, emptyMetricLabel());
        if (!formatted.equals(emptyMetricLabel())) {
            return formatted;
        }
        return findHighlightValue(summary, key);
    }

    private String resolveCpuMeta(WidgetSummary summary, ResourceSnapshot snapshot) {
        Boolean agent = SnapshotTextFormatter.findMetricBoolean(snapshot, "state.agent");
        if (Boolean.TRUE.equals(agent)) {
            return activity.getString(R.string.metric_status_guest_agent_ready);
        }
        if (Boolean.FALSE.equals(agent)) {
            return activity.getString(R.string.metric_status_guest_agent_unavailable);
        }
        return resolveWorkspaceLabel(summary);
    }

    private String resolveTrafficFallback(WidgetSummary summary) {
        String remaining = findHighlightValue(summary, "usage.traffic_remaining_bytes");
        if (!remaining.equals(emptyMetricLabel())) {
            return remaining;
        }
        return findHighlightValue(summary, "usage.traffic_total_bytes");
    }

    private String resolveTrafficValue(WidgetSummary summary, ResourceSnapshot snapshot) {
        String formatted = SnapshotTextFormatter.formatUsageMetricBytes(
                snapshot,
                "usage.traffic_total_bytes",
                "quota.traffic_bytes"
        );
        if (!formatted.equals(emptyMetricLabel())) {
            return formatted;
        }
        return resolveTrafficFallback(summary);
    }

    private String resolveUsageValue(
            WidgetSummary summary,
            ResourceSnapshot snapshot,
            String usageKey,
            String quotaKey
    ) {
        String formatted = SnapshotTextFormatter.formatUsageMetricBytes(snapshot, usageKey, quotaKey);
        if (!formatted.equals(emptyMetricLabel())) {
            return formatted;
        }
        return findHighlightValue(summary, usageKey);
    }

    private String resolveTrafficMeta(ResourceSnapshot snapshot) {
        Long remainingBytes = SnapshotTextFormatter.findMetricLong(snapshot, "usage.traffic_remaining_bytes");
        if (remainingBytes != null) {
            return activity.getString(
                    R.string.metric_traffic_remaining_prefix,
                    SnapshotTextFormatter.formatBytes(remainingBytes)
            );
        }
        return activity.getString(
                R.string.metric_quota_prefix,
                SnapshotTextFormatter.formatMetricBytes(snapshot, "quota.traffic_bytes")
        );
    }

    private String resolveMemoryMeta(ResourceSnapshot snapshot) {
        Long availableBytes = SnapshotTextFormatter.findMetricLong(snapshot, "usage.memory_available_bytes");
        if (availableBytes != null) {
            return activity.getString(
                    R.string.metric_memory_available_prefix,
                    SnapshotTextFormatter.formatBytes(availableBytes)
            );
        }
        return activity.getString(
                R.string.metric_memory_total_prefix,
                SnapshotTextFormatter.formatMetricBytes(snapshot, "usage.memory_total_bytes")
        );
    }

    private Integer resolveTrafficPercent(ResourceSnapshot snapshot) {
        Integer usagePercent = SnapshotTextFormatter.resolveUsagePercent(
                snapshot,
                "usage.traffic_total_bytes",
                "quota.traffic_bytes"
        );
        if (usagePercent != null) {
            return usagePercent;
        }

        Long remainingBytes = SnapshotTextFormatter.findMetricLong(snapshot, "usage.traffic_remaining_bytes");
        Long quotaBytes = SnapshotTextFormatter.findMetricLong(snapshot, "quota.traffic_bytes");
        if (remainingBytes == null || quotaBytes == null || quotaBytes <= 0L) {
            return null;
        }
        long usedBytes = Math.max(0L, quotaBytes - remainingBytes);
        long rounded = Math.round(usedBytes * 100.0 / quotaBytes);
        return (int) Math.min(100L, rounded);
    }

    private Integer resolvePercentMetric(ResourceSnapshot snapshot, String key) {
        String raw = SnapshotTextFormatter.findMetricText(snapshot, key, "");
        if (raw.isBlank()) {
            return null;
        }
        try {
            long rounded = Math.round(Double.parseDouble(raw));
            if (rounded < 0L) {
                return 0;
            }
            return (int) Math.min(100L, rounded);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void setUsageProgress(ProgressBar progressBar, @Nullable Integer progress) {
        progressBar.setProgress(progress == null ? 0 : progress);
    }

    private void appendSnapshotMetaSegment(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank() || emptyMetricLabel().equals(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("  |  ");
        }
        builder.append(label).append(": ").append(value);
    }

    private String emptyMetricLabel() {
        return activity.getString(R.string.metric_value_unavailable);
    }
}
