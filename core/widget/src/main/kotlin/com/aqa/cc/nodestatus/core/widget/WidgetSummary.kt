package com.aqa.cc.nodestatus.core.widget

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import java.time.Instant

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

fun ResourceSnapshot.toWidgetSummary(maxMetrics: Int = 4): WidgetSummary {
    val highlights = metrics
        .take(maxMetrics)
        .map { metric ->
            val renderedValue = buildString {
                append(metric.value?.raw ?: "unsupported")
                if (metric.unit != null) {
                    append(' ')
                    append(metric.unit)
                }
            }
            WidgetMetric(
                key = metric.key,
                label = metric.label,
                valueText = renderedValue,
            )
        }

    return WidgetSummary(
        resourceId = resourceId,
        displayName = displayName,
        collectedAt = collectedAt,
        highlights = highlights,
    )
}
