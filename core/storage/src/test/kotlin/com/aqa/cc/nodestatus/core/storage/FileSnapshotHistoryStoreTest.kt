package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.time.Instant

class FileSnapshotHistoryStoreTest {
    @Test
    fun append_all_trims_oldest_entries_when_limit_is_exceeded() {
        val file = Files.createTempFile("nodestatus-history", ".properties")
        val store = FileSnapshotHistoryStore(file, maxSnapshotCount = 3)

        store.appendAll(
            listOf(
                snapshot("server-a", "2026-03-25T10:00:00Z"),
                snapshot("server-b", "2026-03-25T10:01:00Z"),
                snapshot("server-c", "2026-03-25T10:02:00Z"),
                snapshot("server-d", "2026-03-25T10:03:00Z"),
            ),
        )

        val history = store.listHistory()

        assertEquals(listOf("server-b", "server-c", "server-d"), history.map { it.resourceId })
    }

    @Test
    fun list_history_filters_by_resource_and_applies_limit_to_latest_samples() {
        val file = Files.createTempFile("nodestatus-history", ".properties")
        val store = FileSnapshotHistoryStore(file, maxSnapshotCount = 10)

        store.appendAll(
            listOf(
                snapshot("server-a", "2026-03-25T10:00:00Z"),
                snapshot("server-b", "2026-03-25T10:01:00Z"),
                snapshot("server-a", "2026-03-25T10:02:00Z"),
                snapshot("server-a", "2026-03-25T10:03:00Z"),
            ),
        )

        val history = store.listHistory(resourceId = "site-primary::server-a", limit = 2)

        assertEquals(
            listOf(
                Instant.parse("2026-03-25T10:02:00Z"),
                Instant.parse("2026-03-25T10:03:00Z"),
            ),
            history.map { it.collectedAt },
        )
    }

    private fun snapshot(resourceId: String, collectedAt: String): ResourceSnapshot {
        val instant = Instant.parse(collectedAt)
        return ResourceSnapshot(
            resourceId = resourceId,
            displayName = resourceId.uppercase(),
            providerFamily = ProviderFamily.VIRT_FUSION,
            resourceKind = ResourceKind.VIRTUAL_MACHINE,
            collectedAt = instant,
            metrics = listOf(
                Metric(
                    key = "state.power",
                    label = "Power state",
                    value = MetricValue("running", MetricValueType.TEXT),
                    supported = true,
                    freshness = Freshness.CURRENT,
                    source = "test",
                    collectedAt = instant,
                ),
            ),
            siteId = "site-primary",
            siteDisplayName = "Primary site",
        )
    }
}
