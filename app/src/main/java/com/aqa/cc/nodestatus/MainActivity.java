package com.aqa.cc.nodestatus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotHistoryStore;
import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore;
import com.aqa.cc.nodestatus.core.widget.SnapshotWidgetDataSource;
import com.aqa.cc.nodestatus.core.widget.WidgetMetric;
import com.aqa.cc.nodestatus.core.widget.WidgetSummary;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private VirtFusionSessionConfigStore sessionConfigStore;
    private DashboardSelectionStore dashboardSelectionStore;
    private TextView foundationScopeValue;
    private TextView snapshotStatusValue;
    private TextView snapshotUpdatedValue;
    private TextView snapshotSourceValue;
    private TextView snapshotHighlightsValue;
    private TextView refreshStatusValue;
    private TextView backgroundStatusValue;
    private TextView trafficTrendValue;
    private TextView trafficTrendDelta;
    private TextView memoryTrendValue;
    private TextView memoryTrendDelta;
    private TextView historyListValue;
    private TrendLineView trafficTrendView;
    private TrendLineView memoryTrendView;
    private Spinner serverSelector;
    private ArrayAdapter<String> serverAdapter;
    private EditText baseUrlInput;
    private EditText apiBaseUrlInput;
    private EditText apiTokenInput;
    private EditText cookieHeaderInput;
    private EditText xsrfHeaderInput;
    private EditText userAgentInput;
    private MaterialSwitch allowInsecureTlsSwitch;
    private MaterialSwitch notificationsEnabledSwitch;
    private EditText trafficThresholdInput;
    private Button refreshButton;
    private LinearLayout dashboardSection;
    private LinearLayout settingsSection;
    private List<WidgetSummary> currentSummaries = Collections.emptyList();
    private String selectedResourceId;
    private boolean suppressServerSelectionEvents;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionConfigStore = new VirtFusionSessionConfigStore(this);
        dashboardSelectionStore = new DashboardSelectionStore(this);

        foundationScopeValue = findViewById(R.id.foundationScopeValue);
        snapshotStatusValue = findViewById(R.id.snapshotStatusValue);
        snapshotUpdatedValue = findViewById(R.id.snapshotUpdatedValue);
        snapshotSourceValue = findViewById(R.id.snapshotSourceValue);
        snapshotHighlightsValue = findViewById(R.id.snapshotHighlightsValue);
        refreshStatusValue = findViewById(R.id.refreshStatusValue);
        backgroundStatusValue = findViewById(R.id.backgroundStatusValue);
        trafficTrendValue = findViewById(R.id.trafficTrendValue);
        trafficTrendDelta = findViewById(R.id.trafficTrendDelta);
        memoryTrendValue = findViewById(R.id.memoryTrendValue);
        memoryTrendDelta = findViewById(R.id.memoryTrendDelta);
        historyListValue = findViewById(R.id.historyListValue);
        trafficTrendView = findViewById(R.id.trafficTrendView);
        memoryTrendView = findViewById(R.id.memoryTrendView);
        serverSelector = findViewById(R.id.serverSelector);
        baseUrlInput = findViewById(R.id.baseUrlInput);
        apiBaseUrlInput = findViewById(R.id.apiBaseUrlInput);
        apiTokenInput = findViewById(R.id.apiTokenInput);
        cookieHeaderInput = findViewById(R.id.cookieHeaderInput);
        xsrfHeaderInput = findViewById(R.id.xsrfHeaderInput);
        userAgentInput = findViewById(R.id.userAgentInput);
        allowInsecureTlsSwitch = findViewById(R.id.allowInsecureTlsSwitch);
        notificationsEnabledSwitch = findViewById(R.id.notificationsEnabledSwitch);
        trafficThresholdInput = findViewById(R.id.trafficThresholdInput);
        refreshButton = findViewById(R.id.refreshButton);
        dashboardSection = findViewById(R.id.dashboardSection);
        settingsSection = findViewById(R.id.settingsSection);

        selectedResourceId = dashboardSelectionStore.loadSelectedResourceId();
        setupTabs();
        setupServerSelector();
        foundationScopeValue.setText("VIRT_FUSION");
        loadSessionConfigIntoViews();
        renderLatestSummary();
        renderBackgroundStatus();
        refreshStatusValue.setText(R.string.refresh_status_idle);
        NodeStatusRefreshCoordinator.schedulePeriodicRefresh(this, sessionConfigStore.load());

        refreshButton.setOnClickListener(view -> triggerManualRefresh());
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }

    private void setupTabs() {
        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.topTabGroup);
        toggleGroup.check(R.id.dashboardTabButton);
        setDashboardVisible(true);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            setDashboardVisible(checkedId == R.id.dashboardTabButton);
        });
    }

    private void setupServerSelector() {
        serverAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        serverAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serverSelector.setAdapter(serverAdapter);
        serverSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressServerSelectionEvents || position < 0 || position >= currentSummaries.size()) {
                    return;
                }
                WidgetSummary summary = currentSummaries.get(position);
                selectedResourceId = summary.getResourceId();
                dashboardSelectionStore.saveSelectedResourceId(selectedResourceId);
                renderSelectedSummary(summary);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    private void setDashboardVisible(boolean dashboardVisible) {
        dashboardSection.setVisibility(dashboardVisible ? View.VISIBLE : View.GONE);
        settingsSection.setVisibility(dashboardVisible ? View.GONE : View.VISIBLE);
    }

    private void loadSessionConfigIntoViews() {
        VirtFusionSessionConfig config = sessionConfigStore.load();
        baseUrlInput.setText(config.getBaseUrl());
        apiBaseUrlInput.setText(config.getApiBaseUrl());
        apiTokenInput.setText(config.getApiToken());
        cookieHeaderInput.setText(config.getCookieHeader());
        xsrfHeaderInput.setText(config.getXsrfHeader());
        userAgentInput.setText(config.getUserAgent());
        allowInsecureTlsSwitch.setChecked(config.isAllowInsecureTls());
        notificationsEnabledSwitch.setChecked(config.isNotificationsEnabled());
        trafficThresholdInput.setText(String.valueOf(config.getLowTrafficThresholdPercent()));
    }

    private VirtFusionSessionConfig readSessionConfigFromViews() {
        return new VirtFusionSessionConfig(
                baseUrlInput.getText().toString().trim(),
                apiBaseUrlInput.getText().toString().trim(),
                apiTokenInput.getText().toString().trim(),
                cookieHeaderInput.getText().toString().trim(),
                xsrfHeaderInput.getText().toString().trim(),
                userAgentInput.getText().toString().trim(),
                allowInsecureTlsSwitch.isChecked(),
                notificationsEnabledSwitch.isChecked(),
                parseThresholdPercent()
        );
    }

    private int parseThresholdPercent() {
        String raw = trafficThresholdInput.getText().toString().trim();
        if (raw.isEmpty()) {
            return 20;
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < 1) return 1;
            if (parsed > 100) return 100;
            return parsed;
        } catch (NumberFormatException ignored) {
            return 20;
        }
    }

    private void triggerManualRefresh() {
        VirtFusionSessionConfig config = readSessionConfigFromViews();
        if (config.getBaseUrl().isEmpty()) {
            refreshStatusValue.setText(R.string.refresh_status_missing_base_url);
            return;
        }
        if (!config.hasApiToken() && !config.hasCompatibilitySession()) {
            refreshStatusValue.setText(R.string.refresh_status_missing_auth);
            return;
        }

        sessionConfigStore.save(config);
        maybeRequestNotificationPermission(config);
        refreshButton.setEnabled(false);
        refreshStatusValue.setText(R.string.refresh_status_running);

        executorService.execute(() -> {
            try {
                NodeStatusRefreshCoordinator.RefreshOutcome outcome =
                        NodeStatusRefreshCoordinator.refresh(
                                getApplicationContext(),
                                config,
                                NodeStatusRefreshCoordinator.SOURCE_MANUAL
                        );

                final String probeSummary = outcome.getTokenProbeSummary();
                final int snapshotCount = outcome.getSnapshotCount();
                final boolean usedToken = outcome.usedToken();
                final boolean usedCompatibilitySession = outcome.usedCompatibilitySession();
                mainHandler.post(() -> {
                    renderLatestSummary();
                    renderBackgroundStatus();
                    if (usedToken && !usedCompatibilitySession) {
                        refreshStatusValue.setText(
                                getString(
                                        R.string.refresh_status_token_only,
                                        sanitizeTokenProbe(probeSummary),
                                        snapshotCount
                                )
                        );
                    } else if (probeSummary != null) {
                        refreshStatusValue.setText(
                                getString(
                                        R.string.refresh_status_success_with_token_probe,
                                        snapshotCount,
                                        sanitizeTokenProbe(probeSummary)
                                )
                        );
                    } else {
                        refreshStatusValue.setText(getString(R.string.refresh_status_success, snapshotCount));
                    }
                    refreshButton.setEnabled(true);
                });
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                if (message == null || message.isBlank()) {
                    message = throwable.getClass().getSimpleName();
                }
                final String finalMessage = message;
                new NodeStatusRefreshStatusStore(getApplicationContext()).saveFailure(
                        NodeStatusRefreshCoordinator.SOURCE_MANUAL,
                        finalMessage,
                        java.time.Instant.now().toString()
                );
                NodeStatusWidgetProvider.refreshAll(getApplicationContext());
                mainHandler.post(() -> {
                    renderBackgroundStatus();
                    refreshStatusValue.setText(getString(R.string.refresh_status_failure, finalMessage));
                    refreshButton.setEnabled(true);
                });
            }
        });
    }

    private String sanitizeTokenProbe(String raw) {
        if (raw == null || raw.isBlank()) {
            return "ok";
        }
        String normalized = raw.replace('\n', ' ').trim();
        if (normalized.length() > 64) {
            return normalized.substring(0, 64) + "...";
        }
        return normalized;
    }

    private void renderLatestSummary() {
        currentSummaries = new SnapshotWidgetDataSource(
                new FileSnapshotStore(AppSnapshotFiles.latestSnapshotPath(this))
        ).loadSummaries(20);

        if (currentSummaries.isEmpty()) {
            serverAdapter.clear();
            snapshotStatusValue.setText(getString(R.string.snapshot_empty_status));
            snapshotUpdatedValue.setText(
                    getString(R.string.snapshot_store_path_prefix, AppSnapshotFiles.latestSnapshotPath(this).toString())
            );
            snapshotSourceValue.setText(R.string.snapshot_source_empty);
            snapshotHighlightsValue.setText(getString(R.string.snapshot_empty_body));
            historyListValue.setText(R.string.history_empty);
            renderTrendCards(null);
            return;
        }

        updateServerSelector();
        renderSelectedSummary(resolveSelectedSummary());
    }

    private void updateServerSelector() {
        List<String> labels = new ArrayList<>();
        int selectedIndex = 0;
        for (int index = 0; index < currentSummaries.size(); index++) {
            WidgetSummary summary = currentSummaries.get(index);
            labels.add(summary.getDisplayName());
            if (summary.getResourceId().equals(selectedResourceId)) {
                selectedIndex = index;
            }
        }

        suppressServerSelectionEvents = true;
        serverAdapter.clear();
        serverAdapter.addAll(labels);
        serverAdapter.notifyDataSetChanged();
        serverSelector.setSelection(selectedIndex, false);
        suppressServerSelectionEvents = false;
    }

    private WidgetSummary resolveSelectedSummary() {
        if (selectedResourceId != null) {
            for (WidgetSummary summary : currentSummaries) {
                if (selectedResourceId.equals(summary.getResourceId())) {
                    return summary;
                }
            }
        }
        WidgetSummary summary = currentSummaries.get(0);
        selectedResourceId = summary.getResourceId();
        dashboardSelectionStore.saveSelectedResourceId(selectedResourceId);
        return summary;
    }

    private void renderSelectedSummary(WidgetSummary summary) {
        snapshotStatusValue.setText(summary.getDisplayName());
        snapshotUpdatedValue.setText(
                getString(R.string.snapshot_updated_prefix, summary.getCollectedAt().toString())
        );
        snapshotSourceValue.setText(resolveSnapshotSourceLabel());

        StringBuilder highlightText = new StringBuilder();
        for (WidgetMetric metric : summary.getHighlights()) {
            if (highlightText.length() > 0) {
                highlightText.append('\n');
            }
            highlightText.append(metric.getLabel())
                    .append(": ")
                    .append(metric.getValueText());
        }
        snapshotHighlightsValue.setText(highlightText.toString());
        renderTrendCards(summary.getResourceId());
        renderHistory(summary.getResourceId());
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

        SnapshotTrendAnalyzer.TrendBundle bundle = new SnapshotTrendAnalyzer(this).load(resourceId);
        SnapshotTrendAnalyzer.TrendCardData traffic = bundle.getTrafficTrend();
        SnapshotTrendAnalyzer.TrendCardData memory = bundle.getMemoryTrend();

        trafficTrendValue.setText(getString(R.string.trend_traffic_label, traffic.getValueLabel()));
        trafficTrendDelta.setText(traffic.getChangeLabel());
        trafficTrendView.setSeries(traffic.getPoints());

        memoryTrendValue.setText(getString(R.string.trend_memory_label, memory.getValueLabel()));
        memoryTrendDelta.setText(memory.getChangeLabel());
        memoryTrendView.setSeries(memory.getPoints());
    }

    private void renderHistory(@Nullable String resourceId) {
        if (resourceId == null) {
            historyListValue.setText(R.string.history_empty);
            return;
        }

        List<ResourceSnapshot> history = new FileSnapshotHistoryStore(
                AppSnapshotFiles.historySnapshotPath(this),
                240
        ).listHistory(resourceId, 6);

        if (history.isEmpty()) {
            historyListValue.setText(R.string.history_empty);
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int index = history.size() - 1; index >= 0; index--) {
            ResourceSnapshot snapshot = history.get(index);
            if (builder.length() > 0) {
                builder.append('\n').append('\n');
            }
            builder.append(snapshot.getCollectedAt()).append('\n');
            builder.append("Power: ").append(findMetricText(snapshot, "state.power", "n/a")).append("  ");
            builder.append("Traffic: ").append(formatMetricBytes(snapshot, "usage.traffic_total_bytes")).append('\n');
            builder.append("Memory: ").append(formatMetricBytes(snapshot, "usage.memory_used_bytes")).append("  ");
            builder.append("Disk: ").append(formatMetricBytes(snapshot, "usage.disk_used_bytes"));
        }
        historyListValue.setText(builder.toString());
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

    private CharSequence resolveSnapshotSourceLabel() {
        NodeStatusRefreshStatusStore.RefreshStatus status =
                new NodeStatusRefreshStatusStore(this).load();
        if (status == null) {
            return getString(R.string.snapshot_source_empty);
        }
        if (status.isUsedToken() && status.isUsedCompatibilitySession()) {
            return getString(R.string.snapshot_source_token_plus_session);
        }
        if (status.isUsedToken()) {
            return getString(R.string.snapshot_source_token);
        }
        if (status.isUsedCompatibilitySession()) {
            return getString(R.string.snapshot_source_session);
        }
        return getString(R.string.snapshot_source_empty);
    }

    private void renderBackgroundStatus() {
        NodeStatusRefreshStatusStore.RefreshStatus status =
                new NodeStatusRefreshStatusStore(this).load();
        if (status == null) {
            backgroundStatusValue.setText(R.string.background_status_empty);
            return;
        }

        int labelRes;
        if (status.isPending()) {
            labelRes = R.string.background_status_pending;
        } else if (status.isSuccess()) {
            labelRes = R.string.background_status_success;
        } else {
            labelRes = R.string.background_status_failure;
        }
        backgroundStatusValue.setText(
                getString(
                        labelRes,
                        status.getSource(),
                        status.getUpdatedAt(),
                        status.getMessage()
                )
        );
    }

    private void maybeRequestNotificationPermission(VirtFusionSessionConfig config) {
        if (!config.isNotificationsEnabled()) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                1001
        );
    }
}
