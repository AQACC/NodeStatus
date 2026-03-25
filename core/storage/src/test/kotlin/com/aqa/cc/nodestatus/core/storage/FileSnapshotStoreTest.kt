package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.file.Files
import java.time.Instant

class FileSnapshotStoreTest {
    @Test
    fun round_trips_snapshots_to_disk() {
        val file = Files.createTempFile("nodestatus-snapshots", ".properties")
        val store = FileSnapshotStore(file)
        val collectedAt = Instant.parse("2026-03-24T15:38:41Z")

        val snapshot = ResourceSnapshot(
            resourceId = "server-1",
            displayName = "VMVM",
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = collectedAt,
            metrics = listOf(
                Metric(
                    key = "state.power",
                    label = "Power state",
                    value = MetricValue("running", MetricValueType.TEXT),
                    supported = true,
                    freshness = Freshness.CURRENT,
                    source = "test",
                    collectedAt = collectedAt,
                ),
                Metric(
                    key = "usage.traffic_total_bytes",
                    label = "Traffic total",
                    value = MetricValue("123", MetricValueType.INTEGER),
                    unit = "bytes",
                    supported = true,
                    freshness = Freshness.CURRENT,
                    source = "test",
                    collectedAt = collectedAt,
                ),
            ),
        )

        store.save(snapshot)

        val restored = store.find("server-1")
        assertNotNull(restored)
        assertEquals("VMVM", restored?.displayName)
        assertEquals(2, restored?.metrics?.size)
        assertEquals("running", restored?.metrics?.first()?.value?.raw)
        assertEquals("123", restored?.metrics?.last()?.value?.raw)
    }
}
