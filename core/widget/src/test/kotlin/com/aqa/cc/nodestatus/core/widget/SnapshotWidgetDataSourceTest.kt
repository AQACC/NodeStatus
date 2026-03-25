package com.aqa.cc.nodestatus.core.widget

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import com.aqa.cc.nodestatus.core.storage.SnapshotStore
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SnapshotWidgetDataSourceTest {
    @Test
    fun widget_data_source_prefers_operational_metrics() {
        val collectedAt = Instant.parse("2026-03-24T15:38:41Z")
        val snapshot = ResourceSnapshot(
            resourceId = "server-1",
            displayName = "VMVM",
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = collectedAt,
            metrics = listOf(
                metric("quota.cpu_cores", "CPU cores", "2", MetricValueType.INTEGER, "cores", collectedAt),
                metric("state.power", "Power state", "running", MetricValueType.TEXT, null, collectedAt),
                metric("usage.traffic_remaining_bytes", "Traffic remaining", "536870912000", MetricValueType.INTEGER, "bytes", collectedAt),
                metric("usage.memory_used_bytes", "Memory used", "280592384", MetricValueType.INTEGER, "bytes", collectedAt),
                metric("usage.disk_used_bytes", "Disk used", "5559681024", MetricValueType.INTEGER, "bytes", collectedAt),
            ),
        )
        val store = object : SnapshotStore {
            override fun save(snapshot: ResourceSnapshot) = Unit
            override fun saveAll(snapshots: List<ResourceSnapshot>) = Unit
            override fun find(resourceId: String): ResourceSnapshot? = snapshot
            override fun listLatest(): List<ResourceSnapshot> = listOf(snapshot)
        }

        val summary = SnapshotWidgetDataSource(store).loadSummaries(limit = 1).single()

        assertEquals("VMVM", summary.displayName)
        assertEquals("state.power", summary.highlights[0].key)
        assertEquals("running", summary.highlights[0].valueText)
        assertEquals("500 GB", summary.highlights[1].valueText)
    }

    private fun metric(
        key: String,
        label: String,
        raw: String,
        type: MetricValueType,
        unit: String?,
        collectedAt: Instant,
    ): Metric = Metric(
        key = key,
        label = label,
        value = MetricValue(raw, type),
        unit = unit,
        supported = true,
        freshness = Freshness.CURRENT,
        source = "test",
        collectedAt = collectedAt,
    )
}
