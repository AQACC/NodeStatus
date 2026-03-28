package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.core.model.Freshness;
import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.MetricValue;
import com.aqa.cc.nodestatus.core.model.MetricValueType;
import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.aqa.cc.nodestatus.core.model.ResourceKind;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.widget.WidgetMetric;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

public class SnapshotTextFormatterTest {
    @Test
    public void format_metric_bytes_falls_back_to_raw_metric_value() {
        ResourceSnapshot snapshot = snapshotWithMetric("usage.traffic_total_bytes", "unknown");

        Assert.assertEquals("unknown", SnapshotTextFormatter.formatMetricBytes(snapshot, "usage.traffic_total_bytes"));
    }

    @Test
    public void build_widget_details_skips_primary_highlight() {
        List<WidgetMetric> highlights = List.of(
                new WidgetMetric("state.power", "Power", "running"),
                new WidgetMetric("usage.traffic_total_bytes", "Traffic", "4.8 GB"),
                new WidgetMetric("usage.memory_used_bytes", "Memory", "3 GB")
        );

        Assert.assertEquals(
                "Traffic: 4.8 GB\nMemory: 3 GB",
                SnapshotTextFormatter.buildWidgetDetails(highlights)
        );
    }

    @Test
    public void build_history_preview_orders_latest_first_and_uses_percentages_when_quota_exists() {
        List<ResourceSnapshot> history = List.of(
                snapshot("server-a", "2026-03-25T10:00:00Z", "1073741824"),
                snapshot("server-a", "2026-03-25T10:05:00Z", "2147483648")
        );

        String preview = SnapshotTextFormatter.buildHistoryPreview(history);
        String latestTimestamp = SnapshotTextFormatter.formatInstant(Instant.parse("2026-03-25T10:05:00Z"));
        String earliestTimestamp = SnapshotTextFormatter.formatInstant(Instant.parse("2026-03-25T10:00:00Z"));

        Assert.assertTrue(preview.indexOf(latestTimestamp) < preview.indexOf(earliestTimestamp));
        Assert.assertTrue(preview.contains("Memory: 2 GB / 2 GB (100%)"));
        Assert.assertTrue(preview.contains("Disk: 6 GB / 8 GB (75%)"));
    }

    private ResourceSnapshot snapshot(String resourceId, String collectedAt, String memoryBytes) {
        Instant instant = Instant.parse(collectedAt);
        return new ResourceSnapshot(
                resourceId,
                "VMVM",
                ProviderFamily.VIRT_FUSION,
                ResourceKind.VIRTUAL_MACHINE,
                instant,
                List.of(
                        metric("state.power", "Power state", "running", MetricValueType.TEXT, null, instant),
                        metric("usage.traffic_total_bytes", "Traffic total", "5181442543", MetricValueType.INTEGER, "bytes", instant),
                        metric("quota.memory_bytes", "Memory quota", "2147483648", MetricValueType.INTEGER, "bytes", instant),
                        metric("usage.memory_used_bytes", "Memory used", memoryBytes, MetricValueType.INTEGER, "bytes", instant),
                        metric("quota.disk_bytes", "Disk quota", "8589934592", MetricValueType.INTEGER, "bytes", instant),
                        metric("usage.disk_used_bytes", "Disk used", "6442450944", MetricValueType.INTEGER, "bytes", instant)
                ),
                "",
                ""
        );
    }

    private ResourceSnapshot snapshotWithMetric(String key, String rawValue) {
        Instant instant = Instant.parse("2026-03-25T10:00:00Z");
        return new ResourceSnapshot(
                "server-a",
                "VMVM",
                ProviderFamily.VIRT_FUSION,
                ResourceKind.VIRTUAL_MACHINE,
                instant,
                List.of(metric(key, "Metric", rawValue, MetricValueType.TEXT, null, instant)),
                "",
                ""
        );
    }

    private Metric metric(
            String key,
            String label,
            String rawValue,
            MetricValueType type,
            String unit,
            Instant collectedAt
    ) {
        return new Metric(
                key,
                label,
                new MetricValue(rawValue, type),
                unit,
                true,
                Freshness.CURRENT,
                "test",
                collectedAt
        );
    }
}