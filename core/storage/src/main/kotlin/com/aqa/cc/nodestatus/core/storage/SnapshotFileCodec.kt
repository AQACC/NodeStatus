package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import java.io.Reader
import java.io.Writer
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal object SnapshotFileCodec {
    private val pathLocks = ConcurrentHashMap<Path, ReentrantLock>()

    fun write(path: Path, snapshots: List<ResourceSnapshot>) {
        withPathLock(path) {
            val normalizedPath = path.toAbsolutePath().normalize()
            val parent = normalizedPath.parent ?: error("Snapshot path must have a parent directory: $normalizedPath")
            Files.createDirectories(parent)

            val properties = Properties().apply {
                setProperty("snapshot.count", snapshots.size.toString())
            }

            snapshots.forEachIndexed { snapshotIndex, snapshot ->
                val prefix = "snapshot.$snapshotIndex"
                properties.setProperty("$prefix.resourceId", snapshot.resourceId)
                properties.setProperty("$prefix.displayName", snapshot.displayName)
                properties.setProperty("$prefix.providerFamily", snapshot.providerFamily.name)
                properties.setProperty("$prefix.resourceKind", snapshot.resourceKind.name)
                properties.setProperty("$prefix.collectedAt", snapshot.collectedAt.toString())
                if (snapshot.siteId.isNotBlank()) {
                    properties.setProperty("$prefix.siteId", snapshot.siteId)
                }
                if (snapshot.siteDisplayName.isNotBlank()) {
                    properties.setProperty("$prefix.siteDisplayName", snapshot.siteDisplayName)
                }
                properties.setProperty("$prefix.metric.count", snapshot.metrics.size.toString())

                snapshot.metrics.forEachIndexed { metricIndex, metric ->
                    val metricPrefix = "$prefix.metric.$metricIndex"
                    properties.setProperty("$metricPrefix.key", metric.key)
                    properties.setProperty("$metricPrefix.label", metric.label)
                    properties.setProperty("$metricPrefix.supported", metric.supported.toString())
                    properties.setProperty("$metricPrefix.freshness", metric.freshness.name)
                    properties.setProperty("$metricPrefix.source", metric.source)
                    properties.setProperty("$metricPrefix.collectedAt", metric.collectedAt.toString())
                    metric.unit?.let { properties.setProperty("$metricPrefix.unit", it) }
                    metric.value?.let {
                        properties.setProperty("$metricPrefix.value.raw", it.raw)
                        properties.setProperty("$metricPrefix.value.type", it.type.name)
                    }
                }
            }

            val tempPath = Files.createTempFile(parent, normalizedPath.fileName.toString(), ".tmp")
            try {
                tempPath.writer().use { writer ->
                    properties.store(writer, "NodeStatus snapshots")
                }
                try {
                    Files.move(
                        tempPath,
                        normalizedPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(tempPath, normalizedPath, StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(tempPath)
            }
        }
    }

    fun read(path: Path): List<ResourceSnapshot> {
        return withPathLock(path) {
            val normalizedPath = path.toAbsolutePath().normalize()
            if (!Files.exists(normalizedPath)) {
                return@withPathLock emptyList()
            }

            val properties = Properties().apply {
                normalizedPath.reader().use(::load)
            }

            val snapshotCount = properties.getProperty("snapshot.count")?.toIntOrNull() ?: 0
            (0 until snapshotCount).map { snapshotIndex ->
                val prefix = "snapshot.$snapshotIndex"
                val metricCount = properties.getProperty("$prefix.metric.count")?.toIntOrNull() ?: 0
                ResourceSnapshot(
                    resourceId = properties.requireValue("$prefix.resourceId", normalizedPath),
                    displayName = properties.requireValue("$prefix.displayName", normalizedPath),
                    providerFamily = ProviderFamily.valueOf(properties.requireValue("$prefix.providerFamily", normalizedPath)),
                    resourceKind = ResourceKind.valueOf(properties.requireValue("$prefix.resourceKind", normalizedPath)),
                    collectedAt = Instant.parse(properties.requireValue("$prefix.collectedAt", normalizedPath)),
                    metrics = (0 until metricCount).map { metricIndex ->
                        val metricPrefix = "$prefix.metric.$metricIndex"
                        val rawValue = properties.getProperty("$metricPrefix.value.raw")
                        val valueType = properties.getProperty("$metricPrefix.value.type")
                        Metric(
                            key = properties.requireValue("$metricPrefix.key", normalizedPath),
                            label = properties.requireValue("$metricPrefix.label", normalizedPath),
                            value = if (rawValue != null && valueType != null) {
                                MetricValue(
                                    raw = rawValue,
                                    type = MetricValueType.valueOf(valueType),
                                )
                            } else {
                                null
                            },
                            unit = properties.getProperty("$metricPrefix.unit"),
                            supported = properties.getProperty("$metricPrefix.supported")?.toBooleanStrictOrNull() ?: true,
                            freshness = properties.getProperty("$metricPrefix.freshness")
                                ?.let(Freshness::valueOf)
                                ?: Freshness.UNKNOWN,
                            source = properties.requireValue("$metricPrefix.source", normalizedPath),
                            collectedAt = Instant.parse(properties.requireValue("$metricPrefix.collectedAt", normalizedPath)),
                        )
                    },
                    siteId = properties.getProperty("$prefix.siteId") ?: "",
                    siteDisplayName = properties.getProperty("$prefix.siteDisplayName") ?: "",
                )
            }
        }
    }

    private fun Path.reader(): Reader = Files.newBufferedReader(this)

    private fun Path.writer(): Writer = Files.newBufferedWriter(this)

    private fun Properties.requireValue(key: String, path: Path): String =
        getProperty(key) ?: error("Missing required stored snapshot value '$key' in $path")

    private inline fun <T> withPathLock(path: Path, block: () -> T): T {
        val normalizedPath = path.toAbsolutePath().normalize()
        val lock = pathLocks.computeIfAbsent(normalizedPath) { ReentrantLock() }
        return lock.withLock(block)
    }
}
