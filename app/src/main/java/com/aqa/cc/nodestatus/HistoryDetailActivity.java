package com.aqa.cc.nodestatus;

import android.os.Bundle;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore;
import com.google.android.material.color.MaterialColors;

import java.util.List;

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
        findViewById(R.id.historyBackButton).setOnClickListener(view -> finish());

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
        textView.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, R.color.ink_500));
        textView.setTextSize(14);
        textView.setPadding(0, 8, 0, 0);
        return textView;
    }

    private View createTimelineItem(ResourceSnapshot snapshot, boolean addTopSpacing) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        if (addTopSpacing) {
            row.setPadding(0, 16, 0, 0);
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        content.setLayoutParams(contentParams);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));
        content.setBackground(createCardBackground());

        TextView timestamp = new TextView(this);
        timestamp.setText(SnapshotTextFormatter.formatInstant(snapshot.getCollectedAt()));
        timestamp.setTextColor(themeColor(androidx.appcompat.R.attr.colorPrimary, R.color.teal_700));
        timestamp.setTextSize(13);
        timestamp.setBackground(createBadgeBackground());
        timestamp.setPadding(dp(12), dp(6), dp(12), dp(6));

        TextView values = new TextView(this);
        values.setText(getString(
                R.string.history_timeline_metrics,
                SnapshotTextFormatter.findMetricText(snapshot, "state.power", "n/a"),
                SnapshotTextFormatter.formatMetricBytes(snapshot, "usage.traffic_total_bytes"),
                SnapshotTextFormatter.formatMetricBytes(snapshot, "usage.memory_used_bytes"),
                SnapshotTextFormatter.formatMetricBytes(snapshot, "usage.disk_used_bytes")
        ));
        values.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface, R.color.ink_950));
        values.setTextSize(15);
        values.setPadding(0, 8, 0, 0);

        TextView meta = new TextView(this);
        meta.setText(getString(
                R.string.history_source_hint,
                SnapshotTextFormatter.findMetricText(snapshot, "meta.primary_ipv4", "stored snapshot")
        ));
        meta.setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant, R.color.ink_500));
        meta.setTextSize(13);
        meta.setPadding(0, 10, 0, 0);

        content.addView(timestamp);
        content.addView(values);
        content.addView(meta);
        row.addView(content);
        return row;
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(themeColor(com.google.android.material.R.attr.colorSurface, R.color.paper_50));
        drawable.setCornerRadius(dp(26));
        drawable.setStroke(dp(1), themeColor(com.google.android.material.R.attr.colorOutlineVariant, R.color.mist_200));
        return drawable;
    }

    private GradientDrawable createBadgeBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(themeColor(com.google.android.material.R.attr.colorSurfaceContainer, R.color.paper_100));
        drawable.setCornerRadius(dp(999));
        return drawable;
    }

    private int themeColor(int attrResId, int fallbackResId) {
        return MaterialColors.getColor(this, attrResId, getColor(fallbackResId));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
