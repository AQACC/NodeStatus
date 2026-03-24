package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import com.aqa.cc.nodestatus.core.model.SiteProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

data class VirtFusionFixtureParseResult(
    val siteProfile: SiteProfile,
    val snapshots: List<ResourceSnapshot>,
)

class VirtFusionFixtureParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) {
    fun parseServerOverviewFixture(raw: String): VirtFusionFixtureParseResult {
        val root = json.parseToJsonElement(raw).jsonObject
        val collectedAt = Instant.parse(root.requireString("collectedAt"))
        val siteObject = root.requireObject("siteProfile")

        val siteProfile = SiteProfile(
            id = siteObject.requireString("id"),
            displayName = siteObject.requireString("displayName"),
            baseUrl = siteObject.requireString("baseUrl"),
            providerFamily = ProviderFamily.VIRT_FUSION,
        )

        val snapshots = root.requireArray("resources").map { resourceElement ->
            resourceElement.jsonObject.toSnapshot(collectedAt)
        }

        return VirtFusionFixtureParseResult(
            siteProfile = siteProfile,
            snapshots = snapshots,
        )
    }

    private fun JsonObject.toSnapshot(collectedAt: Instant): ResourceSnapshot {
        val limits = requireObject("limits")
        val usage = requireObject("usage")
        val trafficQuota = limits.optionalLong("trafficBytes")
        val trafficUsed = usage.optionalLong("trafficUsedBytes")

        return ResourceSnapshot(
            resourceId = requireString("id"),
            displayName = requireString("name"),
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = parseResourceKind(optionalString("kind")),
            collectedAt = collectedAt,
            metrics = listOf(
                textMetric(
                    key = "state.power",
                    label = "Power state",
                    value = optionalString("powerState"),
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "quota.traffic_bytes",
                    label = "Traffic quota",
                    value = trafficQuota,
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "usage.traffic_used_bytes",
                    label = "Traffic used",
                    value = trafficUsed,
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "usage.traffic_remaining_bytes",
                    label = "Traffic remaining",
                    value = trafficQuota?.minus(trafficUsed ?: 0L)?.coerceAtLeast(0L),
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                    supported = trafficQuota != null && trafficUsed != null,
                ),
                decimalMetric(
                    key = "usage.cpu_percent",
                    label = "CPU usage",
                    value = usage.optionalDouble("cpuPercent"),
                    unit = "percent",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "quota.memory_bytes",
                    label = "Memory quota",
                    value = limits.optionalLong("memoryBytes"),
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "usage.memory_used_bytes",
                    label = "Memory used",
                    value = usage.optionalLong("memoryUsedBytes"),
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "quota.disk_bytes",
                    label = "Disk quota",
                    value = limits.optionalLong("diskBytes"),
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
                longMetric(
                    key = "usage.disk_used_bytes",
                    label = "Disk used",
                    value = usage.optionalLong("diskUsedBytes"),
                    unit = "bytes",
                    source = SOURCE,
                    collectedAt = collectedAt,
                ),
            ),
        )
    }

    private fun parseResourceKind(raw: String?): ResourceKind = when (raw) {
        "virtual_machine" -> ResourceKind.VIRTUAL_MACHINE
        "container" -> ResourceKind.CONTAINER
        "node" -> ResourceKind.NODE
        else -> ResourceKind.UNKNOWN
    }

    private fun textMetric(
        key: String,
        label: String,
        value: String?,
        source: String,
        collectedAt: Instant,
        supported: Boolean = value != null,
    ): Metric = Metric(
        key = key,
        label = label,
        value = value?.let { MetricValue(raw = it, type = MetricValueType.TEXT) },
        supported = supported,
        freshness = Freshness.CURRENT,
        source = source,
        collectedAt = collectedAt,
    )

    private fun longMetric(
        key: String,
        label: String,
        value: Long?,
        unit: String,
        source: String,
        collectedAt: Instant,
        supported: Boolean = value != null,
    ): Metric = Metric(
        key = key,
        label = label,
        value = value?.let { MetricValue(raw = it.toString(), type = MetricValueType.INTEGER) },
        unit = unit,
        supported = supported,
        freshness = Freshness.CURRENT,
        source = source,
        collectedAt = collectedAt,
    )

    private fun decimalMetric(
        key: String,
        label: String,
        value: Double?,
        unit: String,
        source: String,
        collectedAt: Instant,
        supported: Boolean = value != null,
    ): Metric = Metric(
        key = key,
        label = label,
        value = value?.let { MetricValue(raw = it.toString(), type = MetricValueType.DECIMAL) },
        unit = unit,
        supported = supported,
        freshness = Freshness.CURRENT,
        source = source,
        collectedAt = collectedAt,
    )

    private fun JsonObject.requireString(key: String): String =
        optionalString(key) ?: error("Missing required string '$key'")

    private fun JsonObject.requireObject(key: String): JsonObject =
        this[key]?.jsonObject ?: error("Missing required object '$key'")

    private fun JsonObject.requireArray(key: String): JsonArray =
        this[key] as? JsonArray ?: error("Missing required array '$key'")

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.optionalLong(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.optionalDouble(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    companion object {
        private const val SOURCE = "fixture:virtfusion.vmvm.synthetic"
    }
}
