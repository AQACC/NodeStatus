package com.aqa.cc.nodestatus;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore;

import java.util.List;
import java.util.Locale;

public class HistoryDetailActivity extends AppCompatActivity {
    public static final String EXTRA_RESOURCE_ID = "resource_id";
    public static final String EXTRA_DISPLAY_NAME = "display_name";

    private TextView titleValue;
    private TextView subtitleValue;
    private TextView trafficTrendValue;
    private TextView trafficTrendDelta;
    private TextView memoryTrendValue;
    private TextView memoryTrendDelta;
    private TrendLineView trafficTrendView;
    private TrendLineView memoryTrendView;
    private LinearLayout historyTimelineContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        titleValue = findViewById(R.id.historyDetailTitleValue);
        subtitleValue = findViewById(R.id.historyDetailSubtitleValue);
        trafficTrendValue = findViewById(R.id.historyDetailTrafficValue);
        trafficTrendDelta = findViewById(R.id.historyDetailTrafficDelta);
        memoryTrendValue = findViewById(R.id.historyDetailMemoryValue);
        memoryTrendDelta = findViewById(R.id.historyDetailMemoryDelta);
        trafficTrendView = findViewById(R.id.historyDetailTrafficTrendView);
        memoryTrendView = findViewById(R.id.historyDetailMemoryTrendView);
        historyTimelineContainer = findViewById(R.id.historyDetailTimelineContainer);

        String resourceId = getIntent().getStringExtra(EXTRA_RESOURCE_ID);
        String displayName = getIntent().getStringExtra(EXTRA_DISPLAY_NAME);
        if (resourceId == null || resourceId.isBlank()) {
            finish();
            return;
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = resourceId;
        }

        titleValue.setText(displayName);
        subtitleValue.setText(getString(R.string.history_detail_subtitle, displayName));

        renderHistory(resourceId);
    }

    private void renderHistory(String resourceId) {
        SnapshotTrendAnalyzer.TrendBundle bundle = new SnapshotTrendAnalyzer(this).load(resourceId);
        SnapshotTrendAnalyzer.TrendCardData traffic = bundle.getTrafficTrend();
        SnapshotTrendAnalyzer.TrendCardData memory = bundle.getMemoryTrend();

        trafficTrendValue.setText(getString(R.string.trend_traffic_label, traffic.getValueLabel()));
        trafficTrendDelta.setText(traffic.getChangeLabel());
        trafficTrendView.setSeries(traffic.getPoints());

        memoryTrendValue.setText(getString(R.string.trend_memory_label, memory.getValueLabel()));
        memoryTrendDelta.setText(memory.getChangeLabel());
        memoryTrendView.setSeries(memory.getPoints());

        List<ResourceSnapshot> history = new FileSnapshotHistoryStore(
                AppSnapshotFiles.historySnapshotPath(this),
                240
        ).listHistory(resourceId, 48);

        historyTimelineContainer.removeAllViews();
        if (history.isEmpty()) {
            historyTimelineContainer.addView(createEmptyMessageView());
            return;
        }

        for (int index = history.size() - 1; index >= 0; index--) {
            ResourceSnapshot snapshot = history.get(index);
            historyTimelineContainer.addView(createTimelineItem(snapshot, index != history.size() - 1));
        }
    }

    private View createEmptyMessageView() {
        TextView textView = new TextView(this);
        textView.setText(R.string.history_detail_empty);
        textView.setTextColor(android.graphics.Color.parseColor("#475569"));
        textView.setTextSize(14);
        return textView;
    }

    private View createTimelineItem(ResourceSnapshot snapshot, boolean addTopSpacing) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        if (addTopSpacing) {
            row.setPadding(0, 16, 0, 0);
        }

        View marker = new View(this);
        LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams(18, 18);
        markerParams.topMargin = 8;
        marker.setLayoutParams(markerParams);
        marker.setBackgroundColor(android.graphics.Color.parseColor("#0EA5E9"));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        contentParams.leftMargin = 16;
        content.setLayoutParams(contentParams);
        content.setPadding(20, 18, 20, 18);
        content.setBackgroundColor(android.graphics.Color.parseColor("#F8FAFC"));

        TextView timestamp = new TextView(this);
        timestamp.setText(snapshot.getCollectedAt().toString());
        timestamp.setTextColor(android.graphics.Color.parseColor("#0369A1"));
        timestamp.setTextSize(13);

        TextView values = new TextView(this);
        values.setText(
                "Power: " + findMetricText(snapshot, "state.power", "n/a") + "\n" +
                        "Traffic: " + formatMetricBytes(snapshot, "usage.traffic_total_bytes") + "\n" +
                        "Memory: " + formatMetricBytes(snapshot, "usage.memory_used_bytes") + "\n" +
                        "Disk: " + formatMetricBytes(snapshot, "usage.disk_used_bytes")
        );
        values.setTextColor(android.graphics.Color.parseColor("#0F172A"));
        values.setTextSize(15);
        values.setPadding(0, 8, 0, 0);

        TextView meta = new TextView(this);
        meta.setText("Source hint: " + findMetricText(snapshot, "meta.primary_ipv4", "stored snapshot"));
        meta.setTextColor(android.graphics.Color.parseColor("#475569"));
        meta.setTextSize(13);
        meta.setPadding(0, 10, 0, 0);

        content.addView(timestamp);
        content.addView(values);
        content.addView(meta);

        row.addView(marker);
        row.addView(content);
        return row;
    }

    private String findMetricText(ResourceSnapshot snapshot, String key, String fallback) {
        for (Metric metric : snapshot.getMetrics()) {
            if (key.equals(metric.getKey()) && metric.getValue() != null) {
                return metric.getValue().getRaw();
            }
        }
        return fallback;
    }

    private String formatMetricBytes(ResourceSnapshot snapshot, String key) {
        for (Metric metric : snapshot.getMetrics()) {
            if (key.equals(metric.getKey()) && metric.getValue() != null) {
                try {
                    return formatBytes(Long.parseLong(metric.getValue().getRaw()));
                } catch (NumberFormatException ignored) {
                    return metric.getValue().getRaw();
                }
            }
        }
        return "n/a";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = -1;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex += 1;
        }
        if (value >= 100 || value % 1.0 == 0.0) {
            return ((int) value) + " " + units[unitIndex];
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex]);
    }
}
