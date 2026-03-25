package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

data class VirtFusionApiServerListEntry(
    val resourceId: String,
    val displayName: String,
    val suspended: Boolean,
    val quotaMemoryBytes: Long?,
    val quotaCpuCores: Long?,
    val quotaTrafficBytes: Long?,
    val primaryStorageBytes: Long?,
    val primaryIpv4Address: String?,
    val trafficCycleStartAt: String?,
    val trafficCycleEndAt: String?,
    val createdAt: String?,
)

data class VirtFusionApiServerDetail(
    val resourceId: String,
    val displayName: String,
    val suspended: Boolean,
    val quotaMemoryBytes: Long?,
    val quotaCpuCores: Long?,
    val quotaTrafficBytes: Long?,
    val primaryStorageBytes: Long?,
    val primaryIpv4Address: String?,
    val trafficCycleStartAt: String?,
    val trafficCycleEndAt: String?,
    val createdAt: String?,
)

class VirtFusionOfficialApiParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) {
    fun parseServerList(raw: String): List<VirtFusionApiServerListEntry> {
        val root = json.parseToJsonElement(raw).jsonObject
        return root.requireArray("data").map { item ->
            item.jsonObject.toListEntry()
        }
    }

    fun parseServerDetail(raw: String): VirtFusionApiServerDetail {
        val root = json.parseToJsonElement(raw).jsonObject
        return root.requireObject("data").toDetail()
    }

    fun buildSnapshot(
        detail: VirtFusionApiServerDetail,
        collectedAt: Instant,
        runtime: VirtFusionRuntimeState? = null,
    ): ResourceSnapshot {
        val memoryUsedBytes = if (runtime?.memoryTotalBytes != null && runtime.memoryAvailableBytes != null) {
            (runtime.memoryTotalBytes - runtime.memoryAvailableBytes).coerceAtLeast(0L)
        } else {
            null
        }

        return ResourceSnapshot(
            resourceId = detail.resourceId,
            displayName = detail.displayName,
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = collectedAt,
            metrics = listOf(
                booleanMetric("state.suspended", "Suspended", detail.suspended, collectedAt),
                textMetric("state.power", "Power state", runtime?.powerState, collectedAt, runtime?.powerState != null),
                booleanMetric("state.running", "Running", runtime?.running, collectedAt),
                integerMetric("quota.cpu_cores", "CPU cores", detail.quotaCpuCores, "cores", collectedAt),
                integerMetric("quota.memory_bytes", "Memory quota", detail.quotaMemoryBytes, "bytes", collectedAt),
                integerMetric("quota.disk_bytes", "Disk quota", detail.primaryStorageBytes, "bytes", collectedAt),
                integerMetric("quota.traffic_bytes", "Traffic quota", detail.quotaTrafficBytes, "bytes", collectedAt),
                textMetric("meta.primary_ipv4", "Primary IPv4", detail.primaryIpv4Address, collectedAt),
                textMetric("meta.traffic_cycle_start_at", "Traffic cycle start", detail.trafficCycleStartAt, collectedAt),
                textMetric("meta.traffic_cycle_end_at", "Traffic cycle end", detail.trafficCycleEndAt, collectedAt),
                textMetric("meta.created_at", "Created at", detail.createdAt, collectedAt),
                integerMetric("usage.memory_total_bytes", "Memory total", runtime?.memoryTotalBytes, "bytes", collectedAt),
                integerMetric("usage.memory_used_bytes", "Memory used", memoryUsedBytes, "bytes", collectedAt),
                integerMetric("usage.disk_used_bytes", "Disk used", runtime?.diskUsedBytes, "bytes", collectedAt),
                integerMetric("usage.traffic_total_bytes", "Traffic total", runtime?.trafficTotalBytes, "bytes", collectedAt),
            ),
        )
    }

    private fun JsonObject.toListEntry(): VirtFusionApiServerListEntry {
        val network = requireObject("network")
        val primary = network.requireObject("primary")
        val ipv4 = primary.requireArray("ipv4").firstOrNull()?.jsonObject
        val storage = requireArray("storage").firstOrNull()?.jsonObject
        val currentMonthlyPeriod = requireObject("currentMonthlyPeriod")

        return VirtFusionApiServerListEntry(
            resourceId = requireString("id"),
            displayName = requireString("name"),
            suspended = optionalBoolean("suspended") ?: false,
            quotaMemoryBytes = parseHumanSize(optionalString("memory")),
            quotaCpuCores = parseCpuCores(optionalString("cpu")),
            quotaTrafficBytes = parseHumanSize(primary.optionalString("limit")),
            primaryStorageBytes = parseHumanSize(storage?.optionalString("capacity")),
            primaryIpv4Address = ipv4?.optionalString("address"),
            trafficCycleStartAt = currentMonthlyPeriod.optionalString("start"),
            trafficCycleEndAt = currentMonthlyPeriod.optionalString("end"),
            createdAt = optionalString("created"),
        )
    }

    private fun JsonObject.toDetail(): VirtFusionApiServerDetail {
        val listEntry = toListEntry()
        return VirtFusionApiServerDetail(
            resourceId = listEntry.resourceId,
            displayName = listEntry.displayName,
            suspended = listEntry.suspended,
            quotaMemoryBytes = listEntry.quotaMemoryBytes,
            quotaCpuCores = listEntry.quotaCpuCores,
            quotaTrafficBytes = listEntry.quotaTrafficBytes,
            primaryStorageBytes = listEntry.primaryStorageBytes,
            primaryIpv4Address = listEntry.primaryIpv4Address,
            trafficCycleStartAt = listEntry.trafficCycleStartAt,
            trafficCycleEndAt = listEntry.trafficCycleEndAt,
            createdAt = listEntry.createdAt,
        )
    }

    private fun parseHumanSize(raw: String?): Long? {
        if (raw == null) return null
        val parts = raw.trim().split(' ')
        if (parts.size != 2) return null
        val amount = parts[0].toDoubleOrNull() ?: return null
        val multiplier = when (parts[1].uppercase()) {
            "KB" -> 1024.0
            "MB" -> 1024.0 * 1024.0
            "GB" -> 1024.0 * 1024.0 * 1024.0
            "TB" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
            else -> return null
        }
        return (amount * multiplier).toLong()
    }

    private fun parseCpuCores(raw: String?): Long? =
        raw?.substringBefore(' ')?.toLongOrNull()

    private fun textMetric(
        key: String,
        label: String,
        value: String?,
        collectedAt: Instant,
        supported: Boolean = value != null,
    ): Metric = Metric(
        key = key,
        label = label,
        value = value?.let { MetricValue(it, MetricValueType.TEXT) },
        supported = supported,
        freshness = Freshness.CURRENT,
        source = SOURCE,
        collectedAt = collectedAt,
    )

    private fun integerMetric(
        key: String,
        label: String,
        value: Long?,
        unit: String,
        collectedAt: Instant,
    ): Metric = Metric(
        key = key,
        label = label,
        value = value?.let { MetricValue(it.toString(), MetricValueType.INTEGER) },
        unit = unit,
        supported = value != null,
        freshness = Freshness.CURRENT,
        source = SOURCE,
        collectedAt = collectedAt,
    )

    private fun booleanMetric(
        key: String,
        label: String,
        value: Boolean?,
        collectedAt: Instant,
    ): Metric = Metric(
        key = key,
        label = label,
        value = value?.let { MetricValue(it.toString(), MetricValueType.BOOLEAN) },
        supported = value != null,
        freshness = Freshness.CURRENT,
        source = SOURCE,
        collectedAt = collectedAt,
    )

    private fun JsonObject.requireArray(key: String): JsonArray =
        this[key]?.jsonArray ?: error("Missing required array '$key'")

    private fun JsonObject.requireObject(key: String): JsonObject =
        this[key]?.jsonObject ?: error("Missing required object '$key'")

    private fun JsonObject.requireString(key: String): String =
        optionalString(key) ?: error("Missing required string '$key'")

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.optionalBoolean(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    companion object {
        private const val SOURCE = "api:virtfusion"
    }
}
