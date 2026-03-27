package com.aqa.cc.nodestatus;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore;

import java.util.ArrayList;
import java.util.List;

public final class SnapshotTrendAnalyzer {
    private final Context context;

    public SnapshotTrendAnalyzer(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    public TrendBundle load(String resourceId) {
        List<ResourceSnapshot> history = new FileSnapshotHistoryStore(
                AppSnapshotFiles.historySnapshotPath(context),
                240
        ).listHistory(resourceId, 24);

        return new TrendBundle(
                buildTrend(history, "usage.traffic_total_bytes", true),
                buildTrend(history, "usage.memory_used_bytes", true)
        );
    }

    @NonNull
    private TrendCardData buildTrend(List<ResourceSnapshot> history, String key, boolean bytes) {
        List<Long> samples = new ArrayList<>();
        for (ResourceSnapshot snapshot : history) {
            Long metricValue = SnapshotTextFormatter.findMetricLong(snapshot, key);
            if (metricValue != null) {
                samples.add(metricValue);
            }
        }

        if (samples.isEmpty()) {
            return new TrendCardData("No history yet", "Refresh a few times to build a trend.", new float[0]);
        }

        long current = samples.get(samples.size() - 1);
        long first = samples.get(0);
        long delta = current - first;
        String currentLabel = bytes ? SnapshotTextFormatter.formatBytes(current) : String.valueOf(current);
        String deltaLabel;
        if (delta == 0L) {
            deltaLabel = "No change across the stored samples.";
        } else if (delta > 0L) {
            deltaLabel = "Up " + (bytes ? SnapshotTextFormatter.formatBytes(delta) : delta) + " vs first sample";
        } else {
            deltaLabel = "Down " + (bytes ? SnapshotTextFormatter.formatBytes(Math.abs(delta)) : Math.abs(delta)) + " vs first sample";
        }

        float[] series = new float[samples.size()];
        for (int index = 0; index < samples.size(); index++) {
            series[index] = samples.get(index);
        }
        return new TrendCardData(currentLabel, deltaLabel, series);
    }

    public static final class TrendBundle {
        private final TrendCardData trafficTrend;
        private final TrendCardData memoryTrend;

        public TrendBundle(TrendCardData trafficTrend, TrendCardData memoryTrend) {
            this.trafficTrend = trafficTrend;
            this.memoryTrend = memoryTrend;
        }

        public TrendCardData getTrafficTrend() {
            return trafficTrend;
        }

        public TrendCardData getMemoryTrend() {
            return memoryTrend;
        }
    }

    public static final class TrendCardData {
        private final String valueLabel;
        private final String changeLabel;
        private final float[] points;

        public TrendCardData(String valueLabel, String changeLabel, float[] points) {
            this.valueLabel = valueLabel;
            this.changeLabel = changeLabel;
            this.points = points;
        }

        public String getValueLabel() {
            return valueLabel;
        }

        public String getChangeLabel() {
            return changeLabel;
        }

        public float[] getPoints() {
            return points;
        }
    }
}
