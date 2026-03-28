package com.aqa.cc.nodestatus;

import com.aqa.cc.nodestatus.core.model.Metric;
import com.aqa.cc.nodestatus.core.model.MetricValueType;
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot;
import com.aqa.cc.nodestatus.core.widget.WidgetMetric;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

final class SnapshotTextFormatter {
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());

    private SnapshotTextFormatter() {
    }

    static String formatInstant(Instant instant) {
        return DISPLAY_TIME_FORMATTER.format(instant);
    }

    static String formatStoredInstant(String raw) {
        try {
            return formatInstant(Instant.parse(raw));
        } catch (Exception ignored) {
            return raw;
        }
    }

    static String formatBytes(long bytes) {
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

    static String findMetricText(ResourceSnapshot snapshot, String key, String fallback) {
        Metric metric = findMetric(snapshot, key);
        if (metric != null && metric.getValue() != null) {
            return metric.getValue().getRaw();
        }
        return fallback;
    }

    static Long findMetricLong(ResourceSnapshot snapshot, String key) {
        Metric metric = findMetric(snapshot, key);
        if (metric != null && metric.getValue() != null) {
            try {
                return Long.parseLong(metric.getValue().getRaw());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    static Boolean findMetricBoolean(ResourceSnapshot snapshot, String key) {
        Metric metric = findMetric(snapshot, key);
        if (metric == null || metric.getValue() == null) {
            return null;
        }
        return Boolean.parseBoolean(metric.getValue().getRaw());
    }

    static String formatMetricBytes(ResourceSnapshot snapshot, String key) {
        Long metricValue = findMetricLong(snapshot, key);
        if (metricValue != null) {
            return formatBytes(metricValue);
        }
        return findMetricText(snapshot, key, "n/a");
    }

    static String formatMetricValue(ResourceSnapshot snapshot, String key, String fallback) {
        Metric metric = findMetric(snapshot, key);
        if (metric == null) {
            return fallback;
        }
        return renderMetric(metric, fallback);
    }

    static String formatUsageMetricBytes(ResourceSnapshot snapshot, String usageKey, String quotaKey) {
        Long usedBytes = findMetricLong(snapshot, usageKey);
        if (usedBytes == null) {
            return findMetricText(snapshot, usageKey, "n/a");
        }
        Long quotaBytes = findMetricLong(snapshot, quotaKey);
        return formatUsageBytes(usedBytes, quotaBytes);
    }

    static Integer resolveUsagePercent(ResourceSnapshot snapshot, String usageKey, String quotaKey) {
        Long usedBytes = findMetricLong(snapshot, usageKey);
        Long quotaBytes = findMetricLong(snapshot, quotaKey);
        if (usedBytes == null || quotaBytes == null || quotaBytes <= 0L) {
            return null;
        }
        long rounded = Math.round(usedBytes * 100.0 / quotaBytes);
        if (rounded < 0L) {
            return 0;
        }
        return (int) Math.min(100L, rounded);
    }

    static String buildHighlightBody(List<WidgetMetric> highlights) {
        StringBuilder builder = new StringBuilder();
        for (WidgetMetric metric : highlights) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(metric.getLabel())
                    .append(": ")
                    .append(metric.getValueText());
        }
        return builder.toString();
    }

    static String buildWidgetDetails(List<WidgetMetric> highlights) {
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

    static String buildHistoryPreview(List<ResourceSnapshot> history) {
        StringBuilder builder = new StringBuilder();
        for (int index = history.size() - 1; index >= 0; index--) {
            ResourceSnapshot snapshot = history.get(index);
            if (builder.length() > 0) {
                builder.append('\n').append('\n');
            }
            builder.append(formatInstant(snapshot.getCollectedAt())).append('\n');
            builder.append("Power: ")
                    .append(findMetricText(snapshot, "state.power", "n/a"))
                    .append("  ");
            builder.append("Traffic: ")
                    .append(formatMetricBytes(snapshot, "usage.traffic_total_bytes"))
                    .append('\n');
            builder.append("Memory: ")
                    .append(formatUsageMetricBytes(snapshot, "usage.memory_used_bytes", "quota.memory_bytes"))
                    .append("  ");
            builder.append("Disk: ")
                    .append(formatUsageMetricBytes(snapshot, "usage.disk_used_bytes", "quota.disk_bytes"));
        }
        return builder.toString();
    }

    private static String formatUsageBytes(long usedBytes, Long quotaBytes) {
        String usedText = formatBytes(usedBytes);
        if (quotaBytes == null || quotaBytes <= 0L) {
            return usedText;
        }
        return usedText + " / " + formatBytes(quotaBytes) + " (" + formatPercent(usedBytes * 100.0 / quotaBytes) + ")";
    }

    private static Metric findMetric(ResourceSnapshot snapshot, String key) {
        for (Metric metric : snapshot.getMetrics()) {
            if (key.equals(metric.getKey())) {
                return metric;
            }
        }
        return null;
    }

    private static String renderMetric(Metric metric, String fallback) {
        if (!metric.getSupported() || metric.getValue() == null) {
            return fallback;
        }

        String raw = metric.getValue().getRaw();
        if ("percent".equals(metric.getUnit())) {
            return formatPercentRaw(raw);
        }
        if ("bytes".equals(metric.getUnit()) && metric.getValue().getType() == MetricValueType.INTEGER) {
            try {
                return formatBytes(Long.parseLong(raw));
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
        if (metric.getValue().getType() == MetricValueType.BOOLEAN) {
            return Boolean.parseBoolean(raw) ? "Yes" : "No";
        }
        if (metric.getUnit() != null && !metric.getUnit().isBlank()) {
            return raw + " " + metric.getUnit();
        }
        return raw;
    }

    private static String formatPercent(double percent) {
        if (!Double.isFinite(percent)) {
            return "n/a";
        }
        if (Math.abs(percent) >= 100 || percent % 1.0 == 0.0) {
            return String.format(Locale.US, "%.0f%%", percent);
        }
        return String.format(Locale.US, "%.1f%%", percent);
    }

    private static String formatPercentRaw(String raw) {
        try {
            return formatPercent(Double.parseDouble(raw));
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }
}
