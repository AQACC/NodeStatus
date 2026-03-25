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
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties

internal object SnapshotFileCodec {
    fun write(path: Path, snapshots: List<ResourceSnapshot>) {
        if (path.parent != null) {
            Files.createDirectories(path.parent)
        }

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

        path.writer().use { writer ->
            properties.store(writer, "NodeStatus snapshots")
        }
    }

    fun read(path: Path): List<ResourceSnapshot> {
        if (!Files.exists(path)) {
            return emptyList()
        }

        val properties = Properties().apply {
            path.reader().use(::load)
        }

        val snapshotCount = properties.getProperty("snapshot.count")?.toIntOrNull() ?: 0
        return (0 until snapshotCount).map { snapshotIndex ->
            val prefix = "snapshot.$snapshotIndex"
            val metricCount = properties.getProperty("$prefix.metric.count")?.toIntOrNull() ?: 0
            ResourceSnapshot(
                resourceId = properties.requireValue("$prefix.resourceId", path),
                displayName = properties.requireValue("$prefix.displayName", path),
                providerFamily = ProviderFamily.valueOf(properties.requireValue("$prefix.providerFamily", path)),
                resourceKind = ResourceKind.valueOf(properties.requireValue("$prefix.resourceKind", path)),
                collectedAt = Instant.parse(properties.requireValue("$prefix.collectedAt", path)),
                metrics = (0 until metricCount).map { metricIndex ->
                    val metricPrefix = "$prefix.metric.$metricIndex"
                    val rawValue = properties.getProperty("$metricPrefix.value.raw")
                    val valueType = properties.getProperty("$metricPrefix.value.type")
                    Metric(
                        key = properties.requireValue("$metricPrefix.key", path),
                        label = properties.requireValue("$metricPrefix.label", path),
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
                        source = properties.requireValue("$metricPrefix.source", path),
                        collectedAt = Instant.parse(properties.requireValue("$metricPrefix.collectedAt", path)),
                    )
                },
            )
        }
    }

    private fun Path.reader(): Reader = Files.newBufferedReader(this)

    private fun Path.writer(): Writer = Files.newBufferedWriter(this)

    private fun Properties.requireValue(key: String, path: Path): String =
        getProperty(key) ?: error("Missing required stored snapshot value '$key' in $path")
}
