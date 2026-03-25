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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

data class VirtFusionRuntimeState(
    val resourceId: String?,
    val powerState: String?,
    val running: Boolean?,
    val memoryTotalBytes: Long?,
    val memoryFreeBytes: Long?,
    val memoryAvailableBytes: Long?,
    val diskUsedBytes: Long?,
    val diskCapacityBytes: Long?,
    val trafficRxBytes: Long?,
    val trafficTxBytes: Long?,
    val trafficTotalBytes: Long?,
    val netRxBytes: Long?,
    val netTxBytes: Long?,
)

data class VirtFusionServerDetails(
    val resourceId: String,
    val displayName: String,
    val suspended: Boolean,
    val quotaMemoryBytes: Long?,
    val quotaDiskBytes: Long?,
    val quotaTrafficBytes: Long?,
    val quotaCpuCores: Long?,
    val trafficCycleStartAt: String?,
    val trafficCycleEndAt: String?,
    val osName: String?,
)

data class VirtFusionServerListEntry(
    val resourceNumericId: Long?,
    val resourceId: String,
    val displayName: String,
    val ipv4Address: String?,
    val suspended: Boolean,
    val quotaMemoryBytes: Long?,
    val quotaDiskBytes: Long?,
    val quotaTrafficBytes: Long?,
    val quotaCpuCores: Long?,
    val osName: String?,
    val createdAt: String?,
)

class VirtFusionCapturedResponseParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) {
    fun parseServerList(raw: String): List<VirtFusionServerListEntry> {
        val root = json.parseToJsonElement(raw).jsonObject
        return root.requireArray("data").map { item ->
            val server = item.jsonObject
            val os = server.requireObject("os")

            VirtFusionServerListEntry(
                resourceNumericId = server.optionalLong("id"),
                resourceId = server.requireString("uuid"),
                displayName = server.requireString("name"),
                ipv4Address = server.optionalString("ipv4"),
                suspended = server.optionalFlag("suspended") ?: false,
                quotaMemoryBytes = server.optionalLong("memory")?.times(MIB),
                quotaDiskBytes = server.optionalLong("storage")?.times(GIB),
                quotaTrafficBytes = server.optionalLong("traffic")?.times(GIB),
                quotaCpuCores = server.optionalLong("cpu_cores"),
                osName = os.optionalString("name"),
                createdAt = server.optionalString("created_at"),
            )
        }
    }

    fun parseRuntimeState(raw: String): VirtFusionRuntimeState {
        val root = json.parseToJsonElement(raw).jsonObject
        val data = root.requireObject("data")
        val state = data.requireObject("state")
        val memory = state.requireObject("memory")
        val disk = state.requireObject("disk").firstObjectOrNull()
        val traffic = state.requireObject("traffic").requireObject("total")
        val network = state.requireObject("network").firstObjectOrNull()

        return VirtFusionRuntimeState(
            resourceId = data.optionalString("location")?.substringAfterLast('/'),
            powerState = state.optionalString("state"),
            running = state.optionalBoolean("running"),
            memoryTotalBytes = memory.optionalLong("memtotal")?.times(KIB),
            memoryFreeBytes = memory.optionalLong("memfree")?.times(KIB),
            memoryAvailableBytes = memory.optionalLong("memavailable")?.times(KIB),
            diskUsedBytes = disk?.optionalLong("allocation"),
            diskCapacityBytes = disk?.optionalLong("capacity"),
            trafficRxBytes = traffic.optionalLong("rx"),
            trafficTxBytes = traffic.optionalLong("tx"),
            trafficTotalBytes = traffic.optionalLong("total"),
            netRxBytes = network?.optionalLong("rx.bytes"),
            netTxBytes = network?.optionalLong("tx.bytes"),
        )
    }

    fun parseServerDetails(raw: String): VirtFusionServerDetails {
        val root = json.parseToJsonElement(raw).jsonObject
        val data = root.requireObject("data")
        val resources = data.requireObject("resources")
        val traffic = data.requireObject("traffic")
        val os = data.requireObject("os")

        return VirtFusionServerDetails(
            resourceId = data.requireString("uuid"),
            displayName = data.requireString("name"),
            suspended = data.optionalFlag("suspended") ?: false,
            quotaMemoryBytes = resources.optionalLong("memory")?.times(MIB),
            quotaDiskBytes = resources.optionalLong("storage")?.times(GIB),
            quotaTrafficBytes = resources.optionalLong("traffic")?.times(GIB),
            quotaCpuCores = resources.optionalLong("cpu_cores"),
            trafficCycleStartAt = traffic.optionalString("start"),
            trafficCycleEndAt = traffic.optionalString("end"),
            osName = os.optionalString("name"),
        )
    }

    fun buildSnapshot(
        details: VirtFusionServerDetails,
        runtime: VirtFusionRuntimeState,
        collectedAt: Instant,
    ): ResourceSnapshot {
        if (runtime.resourceId != null && runtime.resourceId != details.resourceId) {
            error("Runtime state belongs to '${runtime.resourceId}', expected '${details.resourceId}'")
        }

        val memoryUsedBytes = if (runtime.memoryTotalBytes != null && runtime.memoryAvailableBytes != null) {
            (runtime.memoryTotalBytes - runtime.memoryAvailableBytes).coerceAtLeast(0L)
        } else {
            null
        }

        return ResourceSnapshot(
            resourceId = details.resourceId,
            displayName = details.displayName,
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = collectedAt,
            metrics = listOf(
                textMetric("state.power", "Power state", runtime.powerState, collectedAt),
                booleanMetric("state.running", "Running", runtime.running, collectedAt),
                booleanMetric("state.suspended", "Suspended", details.suspended, collectedAt),
                integerMetric("quota.cpu_cores", "CPU cores", details.quotaCpuCores, "cores", collectedAt),
                integerMetric("quota.memory_bytes", "Memory quota", details.quotaMemoryBytes, "bytes", collectedAt),
                integerMetric("usage.memory_total_bytes", "Memory total", runtime.memoryTotalBytes, "bytes", collectedAt),
                integerMetric("usage.memory_free_bytes", "Memory free", runtime.memoryFreeBytes, "bytes", collectedAt),
                integerMetric("usage.memory_available_bytes", "Memory available", runtime.memoryAvailableBytes, "bytes", collectedAt),
                integerMetric("usage.memory_used_bytes", "Memory used", memoryUsedBytes, "bytes", collectedAt),
                integerMetric("quota.disk_bytes", "Disk quota", details.quotaDiskBytes ?: runtime.diskCapacityBytes, "bytes", collectedAt),
                integerMetric("usage.disk_used_bytes", "Disk used", runtime.diskUsedBytes, "bytes", collectedAt),
                integerMetric("quota.traffic_bytes", "Traffic quota", details.quotaTrafficBytes, "bytes", collectedAt),
                integerMetric("usage.traffic_rx_bytes", "Traffic RX", runtime.trafficRxBytes, "bytes", collectedAt),
                integerMetric("usage.traffic_tx_bytes", "Traffic TX", runtime.trafficTxBytes, "bytes", collectedAt),
                integerMetric("usage.traffic_total_bytes", "Traffic total", runtime.trafficTotalBytes, "bytes", collectedAt),
                integerMetric("usage.net_rx_bytes", "Network RX", runtime.netRxBytes, "bytes", collectedAt),
                integerMetric("usage.net_tx_bytes", "Network TX", runtime.netTxBytes, "bytes", collectedAt),
                textMetric("meta.traffic_cycle_start_at", "Traffic cycle start", details.trafficCycleStartAt, collectedAt),
                textMetric("meta.traffic_cycle_end_at", "Traffic cycle end", details.trafficCycleEndAt, collectedAt),
                textMetric("meta.os_name", "Operating system", details.osName, collectedAt),
            ),
        )
    }

    private fun textMetric(key: String, label: String, value: String?, collectedAt: Instant): Metric =
        Metric(
            key = key,
            label = label,
            value = value?.let { MetricValue(it, MetricValueType.TEXT) },
            supported = value != null,
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
        this[key] as? JsonArray ?: error("Missing required array '$key'")

    private fun JsonObject.requireObject(key: String): JsonObject =
        this[key]?.jsonObject ?: error("Missing required object '$key'")

    private fun JsonObject.requireString(key: String): String =
        optionalString(key) ?: error("Missing required string '$key'")

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.optionalLong(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.optionalBoolean(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.optionalFlag(key: String): Boolean? {
        val primitive = this[key]?.jsonPrimitive ?: return null
        primitive.booleanOrNull?.let { return it }
        primitive.longOrNull?.let { return it != 0L }
        return when (primitive.contentOrNull?.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }

    private fun JsonObject.firstObjectOrNull(): JsonObject? =
        values.firstOrNull()?.jsonObject

    companion object {
        private const val KIB = 1024L
        private const val MIB = 1024L * 1024L
        private const val GIB = 1024L * 1024L * 1024L
        private const val SOURCE = "fixture:virtfusion.vmvm.sanitized-capture"
    }
}
