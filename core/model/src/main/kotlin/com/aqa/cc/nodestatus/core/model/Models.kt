package com.aqa.cc.nodestatus.core.model

import java.time.Instant

enum class ProviderFamily {
    VIRT_FUSION,
    UNKNOWN,
}

enum class ResourceKind {
    VIRTUAL_MACHINE,
    CONTAINER,
    NODE,
    UNKNOWN,
}

enum class MetricValueType {
    INTEGER,
    DECIMAL,
    BOOLEAN,
    TEXT,
}

enum class Freshness {
    CURRENT,
    STALE,
    UNKNOWN,
}

data class MetricValue(
    val raw: String,
    val type: MetricValueType,
)

data class Metric(
    val key: String,
    val label: String,
    val value: MetricValue? = null,
    val unit: String? = null,
    val supported: Boolean = true,
    val freshness: Freshness = Freshness.UNKNOWN,
    val source: String,
    val collectedAt: Instant,
)

data class SiteProfile(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val providerFamily: ProviderFamily,
)

data class ResourceSnapshot(
    val resourceId: String,
    val displayName: String,
    val providerFamily: ProviderFamily,
    val resourceKind: ResourceKind,
    val collectedAt: Instant,
    val metrics: List<Metric>,
)
