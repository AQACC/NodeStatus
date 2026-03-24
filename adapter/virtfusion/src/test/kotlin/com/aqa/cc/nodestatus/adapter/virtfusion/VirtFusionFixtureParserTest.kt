package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class VirtFusionFixtureParserTest {
    private val parser = VirtFusionFixtureParser()

    @Test
    fun synthetic_fixture_maps_to_resource_snapshots() {
        val fixturePath = fixturePath("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-overview.synthetic.json")
        val parsed = parser.parseServerOverviewFixture(Files.readString(fixturePath))

        assertEquals("virtfusion.vmvm", parsed.siteProfile.id)
        assertEquals("vmvm", parsed.siteProfile.displayName)
        assertEquals(ProviderFamily.VIRT_FUSION, parsed.siteProfile.providerFamily)
        assertEquals(2, parsed.snapshots.size)

        val firstSnapshot = parsed.snapshots[0]
        assertEquals("vm-101", firstSnapshot.resourceId)
        assertEquals("vmvm-demo-1", firstSnapshot.displayName)
        assertEquals(ResourceKind.VIRTUAL_MACHINE, firstSnapshot.resourceKind)

        val firstMetrics = firstSnapshot.metrics.associateBy { it.key }
        assertEquals("running", firstMetrics.getValue("state.power").value?.raw)
        assertEquals("1000000000000", firstMetrics.getValue("quota.traffic_bytes").value?.raw)
        assertEquals("250000000000", firstMetrics.getValue("usage.traffic_used_bytes").value?.raw)
        assertEquals("750000000000", firstMetrics.getValue("usage.traffic_remaining_bytes").value?.raw)
        assertEquals("23.7", firstMetrics.getValue("usage.cpu_percent").value?.raw)
        assertTrue(firstMetrics.getValue("usage.cpu_percent").supported)

        val secondSnapshot = parsed.snapshots[1]
        val secondMetrics = secondSnapshot.metrics.associateBy { it.key }
        assertEquals("stopped", secondMetrics.getValue("state.power").value?.raw)
        assertEquals("375000000000", secondMetrics.getValue("usage.traffic_remaining_bytes").value?.raw)
        assertFalse(secondMetrics.getValue("usage.cpu_percent").supported)
        assertEquals(null, secondMetrics.getValue("usage.cpu_percent").value)
        assertFalse(secondMetrics.getValue("usage.memory_used_bytes").supported)
        assertNotNull(secondMetrics.getValue("usage.disk_used_bytes").value)
    }

    private fun fixturePath(vararg parts: String): Path {
        val repoRoot = System.getProperty("nodestatus.repoRoot")
        require(!repoRoot.isNullOrBlank()) { "Missing nodestatus.repoRoot system property" }
        return Path.of(repoRoot, *parts)
    }
}
