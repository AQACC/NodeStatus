package com.aqa.cc.nodestatus.core.widget

import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import com.aqa.cc.nodestatus.core.storage.SnapshotStore
import java.time.Instant
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
)

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
    val preferredKeys = listOf(
        "state.power",
        "usage.traffic_remaining_bytes",
        "usage.traffic_total_bytes",
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
                valueText = metric.renderValue(),
            )
        },
    )
}

private fun Metric.renderValue(): String {
    val metricValue = value
    if (!supported || metricValue == null) {
        return "unsupported"
    }

    return when {
        unit == "bytes" && metricValue.type == MetricValueType.INTEGER -> formatBytes(metricValue.raw.toLongOrNull())
        metricValue.type == MetricValueType.BOOLEAN -> if (metricValue.raw.equals("true", ignoreCase = true)) "Yes" else "No"
        unit != null -> "${metricValue.raw} $unit"
        else -> metricValue.raw
    }
}

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
        "%.1f".format(value)
    }
    return "$rounded ${units[unitIndex]}"
}
