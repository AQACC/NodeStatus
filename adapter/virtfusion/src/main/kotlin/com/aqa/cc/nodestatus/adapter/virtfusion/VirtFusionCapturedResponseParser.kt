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
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.Instant

data class VirtFusionRuntimeDiskStats(
    val device: String,
    val name: String?,
    val rdReqs: Long?,
    val rdBytes: Long?,
    val rdTimes: Long?,
    val wrReqs: Long?,
    val wrBytes: Long?,
    val wrTimes: Long?,
    val flReqs: Long?,
    val flTimes: Long?,
    val allocationBytes: Long?,
    val capacityBytes: Long?,
    val physicalBytes: Long?,
)

data class VirtFusionRuntimeNetworkStats(
    val interfaceName: String,
    val name: String?,
    val rxBytes: Long?,
    val rxPkts: Long?,
    val rxErrs: Long?,
    val rxDrop: Long?,
    val txBytes: Long?,
    val txPkts: Long?,
    val txErrs: Long?,
    val txDrop: Long?,
)

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
    val queue: Boolean? = null,
    val commissioned: Long? = null,
    val backupLevel: Long? = null,
    val pendingActions: Boolean? = null,
    val protected: Boolean? = null,
    val destroyable: Boolean? = null,
    val cpu: Double? = null,
    val agent: Boolean? = null,
    val memoryBuffersBytes: Long? = null,
    val memoryCachedBytes: Long? = null,
    val memorySreclaimableBytes: Long? = null,
    val memorySource: String? = null,
    val trafficSql: Boolean? = null,
    val errors: List<String> = emptyList(),
    val disks: List<VirtFusionRuntimeDiskStats> = emptyList(),
    val networks: List<VirtFusionRuntimeNetworkStats> = emptyList(),
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
    val primaryIpv4Address: String?,
    val createdAt: String?,
    val osDist: String?,
    val osKernel: String?,
    val osUpdatedAt: String?,
    val trafficExceeded: Boolean?,
    val cpuModel: String?,
    val vncEnabled: Boolean?,
    val vncIp: String?,
    val vncPort: Long?,
    val timezone: String?,
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
        val disks = state.requireObject("disk")
        val traffic = state.requireObject("traffic").requireObject("total")
        val networks = state.requireObject("network")
        val primaryDisk = disks.firstObjectOrNull()
        val primaryNetwork = networks.firstObjectOrNull()

        val diskStats = disks.entries
            .sortedBy { it.key }
            .map { (device, value) ->
                val diskObject = value.jsonObject
                VirtFusionRuntimeDiskStats(
                    device = device,
                    name = diskObject.optionalString("name"),
                    rdReqs = diskObject.optionalLong("rd.reqs"),
                    rdBytes = diskObject.optionalLong("rd.bytes"),
                    rdTimes = diskObject.optionalLong("rd.times"),
                    wrReqs = diskObject.optionalLong("wr.reqs"),
                    wrBytes = diskObject.optionalLong("wr.bytes"),
                    wrTimes = diskObject.optionalLong("wr.times"),
                    flReqs = diskObject.optionalLong("fl.reqs"),
                    flTimes = diskObject.optionalLong("fl.times"),
                    allocationBytes = diskObject.optionalLong("allocation"),
                    capacityBytes = diskObject.optionalLong("capacity"),
                    physicalBytes = diskObject.optionalLong("physical"),
                )
            }
        val networkStats = networks.entries
            .sortedBy { it.key }
            .map { (interfaceName, value) ->
                val networkObject = value.jsonObject
                VirtFusionRuntimeNetworkStats(
                    interfaceName = interfaceName,
                    name = networkObject.optionalString("name"),
                    rxBytes = networkObject.optionalLong("rx.bytes"),
                    rxPkts = networkObject.optionalLong("rx.pkts"),
                    rxErrs = networkObject.optionalLong("rx.errs"),
                    rxDrop = networkObject.optionalLong("rx.drop"),
                    txBytes = networkObject.optionalLong("tx.bytes"),
                    txPkts = networkObject.optionalLong("tx.pkts"),
                    txErrs = networkObject.optionalLong("tx.errs"),
                    txDrop = networkObject.optionalLong("tx.drop"),
                )
            }

        return VirtFusionRuntimeState(
            resourceId = data.optionalString("location")?.substringAfterLast('/'),
            powerState = state.optionalString("state"),
            running = state.optionalBoolean("running"),
            memoryTotalBytes = memory.optionalLong("memtotal")?.times(KIB),
            memoryFreeBytes = memory.optionalLong("memfree")?.times(KIB),
            memoryAvailableBytes = memory.optionalLong("memavailable")?.times(KIB),
            diskUsedBytes = primaryDisk?.optionalLong("allocation"),
            diskCapacityBytes = primaryDisk?.optionalLong("capacity"),
            trafficRxBytes = traffic.optionalLong("rx"),
            trafficTxBytes = traffic.optionalLong("tx"),
            trafficTotalBytes = traffic.optionalLong("total"),
            netRxBytes = primaryNetwork?.optionalLong("rx.bytes"),
            netTxBytes = primaryNetwork?.optionalLong("tx.bytes"),
            queue = data.optionalFlag("queue"),
            commissioned = data.optionalLong("commissioned"),
            backupLevel = data.optionalLong("backup_level"),
            pendingActions = data.optionalFlag("pendingActions"),
            protected = data.optionalFlag("protected"),
            destroyable = data.optionalFlag("destroyable"),
            cpu = state.optionalDouble("cpu"),
            agent = state.optionalFlag("agent"),
            memoryBuffersBytes = memory.optionalLong("buffers")?.times(KIB),
            memoryCachedBytes = memory.optionalLong("cached")?.times(KIB),
            memorySreclaimableBytes = memory.optionalLong("sreclaimable")?.times(KIB),
            memorySource = memory.optionalString("_source"),
            trafficSql = traffic.optionalFlag("sql"),
            errors = state.optionalArray("errors")
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?: emptyList(),
            disks = diskStats,
            networks = networkStats,
        )
    }

    fun parseServerDetails(raw: String): VirtFusionServerDetails {
        val root = json.parseToJsonElement(raw).jsonObject
        val data = root.requireObject("data")
        val resources = data.requireObject("resources")
        val traffic = data.requireObject("traffic")
        val os = data.requireObject("os")
        val vnc = data.optionalObject("vnc")
        val primaryInterface = data.requireObject("network")
            .optionalArray("interfaces")
            ?.firstObjectOrNull()
        val primaryIpv4 = primaryInterface
            ?.optionalArray("ipv4")
            ?.firstObjectOrNull()

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
            primaryIpv4Address = primaryIpv4?.optionalString("address"),
            createdAt = data.optionalString("created") ?: data.optionalString("created_at"),
            osDist = os.optionalString("dist"),
            osKernel = os.optionalString("kernel"),
            osUpdatedAt = os.optionalString("updated"),
            trafficExceeded = root.optionalFlag("trafficExceeded"),
            cpuModel = resources.optionalString("cpu_model"),
            vncEnabled = vnc?.optionalFlag("enabled"),
            vncIp = vnc?.optionalString("ip"),
            vncPort = vnc?.optionalLong("port"),
            timezone = data.optionalString("timezone"),
        )
    }

    fun buildSnapshot(
        details: VirtFusionServerDetails,
        runtime: VirtFusionRuntimeState,
        collectedAt: Instant,
    ): ResourceSnapshot {
        if (runtime.resourceId != null && runtime.resourceId != details.resourceId) {
            error("Runtime state belongs to `${runtime.resourceId}`, expected `${details.resourceId}`")
        }

        val memoryUsedBytes = if (runtime.memoryTotalBytes != null && runtime.memoryAvailableBytes != null) {
            (runtime.memoryTotalBytes - runtime.memoryAvailableBytes).coerceAtLeast(0L)
        } else {
            null
        }

        val metrics = buildList {
            add(textMetric("state.power", "Power state", runtime.powerState, collectedAt))
            add(booleanMetric("state.running", "Running", runtime.running, collectedAt))
            add(booleanMetric("state.suspended", "Suspended", details.suspended, collectedAt))
            add(integerMetric("quota.cpu_cores", "CPU cores", details.quotaCpuCores, "cores", collectedAt))
            add(integerMetric("quota.memory_bytes", "Memory quota", details.quotaMemoryBytes, "bytes", collectedAt))
            add(integerMetric("usage.memory_total_bytes", "Memory total", runtime.memoryTotalBytes, "bytes", collectedAt))
            add(integerMetric("usage.memory_free_bytes", "Memory free", runtime.memoryFreeBytes, "bytes", collectedAt))
            add(integerMetric("usage.memory_available_bytes", "Memory available", runtime.memoryAvailableBytes, "bytes", collectedAt))
            add(integerMetric("usage.memory_used_bytes", "Memory used", memoryUsedBytes, "bytes", collectedAt))
            add(integerMetric("quota.disk_bytes", "Disk quota", details.quotaDiskBytes ?: runtime.diskCapacityBytes, "bytes", collectedAt))
            add(integerMetric("usage.disk_used_bytes", "Disk used", runtime.diskUsedBytes, "bytes", collectedAt))
            add(integerMetric("quota.traffic_bytes", "Traffic quota", details.quotaTrafficBytes, "bytes", collectedAt))
            add(integerMetric("usage.traffic_rx_bytes", "Traffic RX", runtime.trafficRxBytes, "bytes", collectedAt))
            add(integerMetric("usage.traffic_tx_bytes", "Traffic TX", runtime.trafficTxBytes, "bytes", collectedAt))
            add(integerMetric("usage.traffic_total_bytes", "Traffic total", runtime.trafficTotalBytes, "bytes", collectedAt))
            add(integerMetric("usage.net_rx_bytes", "Network RX", runtime.netRxBytes, "bytes", collectedAt))
            add(integerMetric("usage.net_tx_bytes", "Network TX", runtime.netTxBytes, "bytes", collectedAt))
            add(textMetric("meta.traffic_cycle_start_at", "Traffic cycle start", details.trafficCycleStartAt, collectedAt))
            add(textMetric("meta.traffic_cycle_end_at", "Traffic cycle end", details.trafficCycleEndAt, collectedAt))
            add(textMetric("meta.os_name", "Operating system", details.osName, collectedAt))

            add(decimalMetric("state.cpu", runtime.cpu, collectedAt, "percent"))
            add(booleanMetric("state.agent", "state.agent", runtime.agent, collectedAt))
            add(booleanMetric("queue", "queue", runtime.queue, collectedAt))
            add(integerMetric("commissioned", "commissioned", runtime.commissioned, null, collectedAt))
            add(integerMetric("backup_level", "backup_level", runtime.backupLevel, null, collectedAt))
            add(booleanMetric("pendingActions", "pendingActions", runtime.pendingActions, collectedAt))
            add(booleanMetric("protected", "protected", runtime.protected, collectedAt))
            add(booleanMetric("destroyable", "destroyable", runtime.destroyable, collectedAt))
            add(integerMetric("state.memory.buffers", "state.memory.buffers", runtime.memoryBuffersBytes, "bytes", collectedAt))
            add(integerMetric("state.memory.cached", "state.memory.cached", runtime.memoryCachedBytes, "bytes", collectedAt))
            add(integerMetric("state.memory.sreclaimable", "state.memory.sreclaimable", runtime.memorySreclaimableBytes, "bytes", collectedAt))
            add(textMetric("state.memory._source", "state.memory._source", runtime.memorySource, collectedAt))
            add(booleanMetric("state.traffic.total.sql", "state.traffic.total.sql", runtime.trafficSql, collectedAt))
            add(textMetric("state.errors", "state.errors", runtime.errors.takeIf { it.isNotEmpty() }?.joinToString(" | "), collectedAt))

            add(textMetric("ipv4", "ipv4", details.primaryIpv4Address, collectedAt))
            add(textMetric("created", "created", details.createdAt, collectedAt))
            add(textMetric("os.dist", "os.dist", details.osDist, collectedAt))
            add(textMetric("os.kernel", "os.kernel", details.osKernel, collectedAt))
            add(textMetric("os.updated", "os.updated", details.osUpdatedAt, collectedAt))
            add(textMetric("resources.cpu_model", "resources.cpu_model", details.cpuModel, collectedAt))
            add(booleanMetric("trafficExceeded", "trafficExceeded", details.trafficExceeded, collectedAt))
            add(booleanMetric("vnc.enabled", "vnc.enabled", details.vncEnabled, collectedAt))
            add(textMetric("vnc.ip", "vnc.ip", details.vncIp, collectedAt))
            add(integerMetric("vnc.port", "vnc.port", details.vncPort, null, collectedAt))
            add(textMetric("timezone", "timezone", details.timezone, collectedAt))

            runtime.disks.forEach { diskStats ->
                add(textMetric("state.disk.${diskStats.device}.name", "state.disk.${diskStats.device}.name", diskStats.name, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.rd.reqs", "state.disk.${diskStats.device}.rd.reqs", diskStats.rdReqs, null, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.rd.bytes", "state.disk.${diskStats.device}.rd.bytes", diskStats.rdBytes, "bytes", collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.rd.times", "state.disk.${diskStats.device}.rd.times", diskStats.rdTimes, null, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.wr.reqs", "state.disk.${diskStats.device}.wr.reqs", diskStats.wrReqs, null, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.wr.bytes", "state.disk.${diskStats.device}.wr.bytes", diskStats.wrBytes, "bytes", collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.wr.times", "state.disk.${diskStats.device}.wr.times", diskStats.wrTimes, null, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.fl.reqs", "state.disk.${diskStats.device}.fl.reqs", diskStats.flReqs, null, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.fl.times", "state.disk.${diskStats.device}.fl.times", diskStats.flTimes, null, collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.allocation", "state.disk.${diskStats.device}.allocation", diskStats.allocationBytes, "bytes", collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.capacity", "state.disk.${diskStats.device}.capacity", diskStats.capacityBytes, "bytes", collectedAt))
                add(integerMetric("state.disk.${diskStats.device}.physical", "state.disk.${diskStats.device}.physical", diskStats.physicalBytes, "bytes", collectedAt))
            }

            runtime.networks.forEach { networkStats ->
                add(textMetric("state.network.${networkStats.interfaceName}.name", "state.network.${networkStats.interfaceName}.name", networkStats.name, collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.rx.bytes", "state.network.${networkStats.interfaceName}.rx.bytes", networkStats.rxBytes, "bytes", collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.rx.pkts", "state.network.${networkStats.interfaceName}.rx.pkts", networkStats.rxPkts, null, collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.rx.errs", "state.network.${networkStats.interfaceName}.rx.errs", networkStats.rxErrs, null, collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.rx.drop", "state.network.${networkStats.interfaceName}.rx.drop", networkStats.rxDrop, null, collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.tx.bytes", "state.network.${networkStats.interfaceName}.tx.bytes", networkStats.txBytes, "bytes", collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.tx.pkts", "state.network.${networkStats.interfaceName}.tx.pkts", networkStats.txPkts, null, collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.tx.errs", "state.network.${networkStats.interfaceName}.tx.errs", networkStats.txErrs, null, collectedAt))
                add(integerMetric("state.network.${networkStats.interfaceName}.tx.drop", "state.network.${networkStats.interfaceName}.tx.drop", networkStats.txDrop, null, collectedAt))
            }
        }

        return ResourceSnapshot(
            resourceId = details.resourceId,
            displayName = details.displayName,
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = collectedAt,
            metrics = metrics,
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
        unit: String?,
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

    private fun decimalMetric(
        key: String,
        value: Double?,
        collectedAt: Instant,
        unit: String? = null,
    ): Metric = Metric(
        key = key,
        label = key,
        value = value?.let { MetricValue(it.toString(), MetricValueType.DECIMAL) },
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

    private fun JsonObject.optionalObject(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonObject.optionalArray(key: String): JsonArray? =
        this[key] as? JsonArray

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.optionalLong(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.optionalDouble(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull
            ?: this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

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

    private fun JsonArray.firstObjectOrNull(): JsonObject? =
        firstOrNull()?.jsonObject

    companion object {
        private const val KIB = 1024L
        private const val MIB = 1024L * 1024L
        private const val GIB = 1024L * 1024L * 1024L
        private const val SOURCE = "fixture:virtfusion.vmvm.sanitized-capture"
    }
}
