package com.aqa.cc.nodestatus.core.widget

import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import com.aqa.cc.nodestatus.core.storage.SnapshotStore
import java.time.Instant
import java.util.Locale
import kotlin.math.abs

data class WidgetMetric(
    val key: String,
    val label: String,
    val valueText: String,
)

data class WidgetSummary(
    val resourceId: String,
    val displayName: String,
    val collectedAt: Instant,
    val highlights: List<WidgetMetric>,
    val siteId: String = "",
    val siteDisplayName: String = "",
) {
    val scopedResourceId: String
        get() = if (siteId.isBlank()) resourceId else "$siteId::$resourceId"
}

class SnapshotWidgetDataSource(
    private val snapshotStore: SnapshotStore,
) {
    fun loadSummaries(limit: Int): List<WidgetSummary> =
        snapshotStore.listLatest()
            .sortedByDescending { it.collectedAt }
            .take(limit)
            .map { it.toWidgetSummary() }
}

fun ResourceSnapshot.toWidgetSummary(maxMetrics: Int = 4): WidgetSummary {
    val trafficKey = when {
        hasMetric("usage.traffic_remaining_bytes") -> "usage.traffic_remaining_bytes"
        hasMetric("usage.traffic_total_bytes") -> "usage.traffic_total_bytes"
        else -> null
    }
    val preferredKeys = listOfNotNull(
        "state.power",
        trafficKey,
        "state.cpu",
        "usage.memory_used_bytes",
        "usage.disk_used_bytes",
    )

    val highlightMetrics = buildList {
        preferredKeys.forEach { preferredKey ->
            metrics.firstOrNull { it.key == preferredKey }?.let(::add)
        }
        if (size < maxMetrics) {
            metrics.forEach { metric ->
                if (none { it.key == metric.key }) {
                    add(metric)
                }
            }
        }
    }.take(maxMetrics)

    return WidgetSummary(
        resourceId = resourceId,
        displayName = displayName,
        collectedAt = collectedAt,
        highlights = highlightMetrics.map { metric ->
            WidgetMetric(
                key = metric.key,
                label = metric.label,
                valueText = renderMetric(metric),
            )
        },
        siteId = siteId,
        siteDisplayName = siteDisplayName,
    )
}

private fun ResourceSnapshot.renderMetric(metric: Metric): String {
    val metricValue = metric.value
    if (!metric.supported || metricValue == null) {
        return "unsupported"
    }

    return when (metric.key) {
        "usage.memory_used_bytes" -> formatUsageBytes(
            metricValue.raw.toLongOrNull(),
            findMetricLong("quota.memory_bytes"),
        )

        "usage.disk_used_bytes" -> formatUsageBytes(
            metricValue.raw.toLongOrNull(),
            findMetricLong("quota.disk_bytes"),
        )

        else -> when {
            metric.unit == "percent" -> formatPercentRaw(metricValue.raw)
            metric.unit == "bytes" && metricValue.type == MetricValueType.INTEGER -> formatBytes(metricValue.raw.toLongOrNull())
            metricValue.type == MetricValueType.BOOLEAN -> if (metricValue.raw.equals("true", ignoreCase = true)) "Yes" else "No"
            metric.unit != null -> "${metricValue.raw} ${metric.unit}"
            else -> metricValue.raw
        }
    }
}

private fun ResourceSnapshot.findMetricLong(key: String): Long? =
    metrics.firstOrNull { it.key == key }?.value?.raw?.toLongOrNull()

private fun ResourceSnapshot.hasMetric(key: String): Boolean =
    metrics.any { it.key == key }

private fun formatUsageBytes(usedBytes: Long?, quotaBytes: Long?): String {
    val usedText = formatBytes(usedBytes)
    if (usedBytes == null || quotaBytes == null || quotaBytes <= 0L) {
        return usedText
    }
    return "$usedText / ${formatBytes(quotaBytes)} (${formatPercent(usedBytes.toDouble() * 100.0 / quotaBytes.toDouble())})"
}

private fun formatPercent(percent: Double): String {
    if (!percent.isFinite()) {
        return "n/a"
    }
    val rounded = if (abs(percent) >= 100 || percent % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", percent)
    } else {
        String.format(Locale.US, "%.1f", percent)
    }
    return "$rounded%"
}

private fun formatPercentRaw(raw: String): String =
    raw.toDoubleOrNull()?.let(::formatPercent) ?: raw

private fun formatBytes(bytes: Long?): String {
    if (bytes == null) {
        return "n/a"
    }
    if (bytes < 1024L) {
        return "$bytes B"
    }

    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }

    val rounded = if (abs(value) >= 100 || value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    return "$rounded ${units[unitIndex]}"
}
