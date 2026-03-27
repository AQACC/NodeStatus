package com.aqa.cc.nodestatus.adapter.virtfusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class VirtFusionCapturedResponseParserTest {
    private val parser = VirtFusionCapturedResponseParser()

    @Test
    fun captured_server_list_response_maps_to_entries() {
        val entries = parser.parseServerList(
            Files.readString(fixturePath("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-list.capture.sanitized.json")),
        )

        assertEquals(1, entries.size)

        val entry = entries.single()
        assertEquals(192L, entry.resourceNumericId)
        assertEquals("11111111-2222-3333-4444-555555555555", entry.resourceId)
        assertEquals("VMVM", entry.displayName)
        assertEquals("203.0.113.170", entry.ipv4Address)
        assertFalse(entry.suspended)
        assertEquals(2147483648L, entry.quotaMemoryBytes)
        assertEquals(42949672960L, entry.quotaDiskBytes)
        assertEquals(536870912000L, entry.quotaTrafficBytes)
        assertEquals(2L, entry.quotaCpuCores)
        assertEquals("Debian GNU/Linux 13 (trixie)", entry.osName)
        assertEquals("2026-03-23T02:09:14.000000Z", entry.createdAt)
    }

    @Test
    fun captured_vmvm_responses_merge_into_a_snapshot() {
        val details = parser.parseServerDetails(
            Files.readString(fixturePath("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-details.capture.sanitized.json")),
        )
        val runtime = parser.parseRuntimeState(
            Files.readString(fixturePath("rule-packs", "packs", "virtfusion.vmvm", "fixtures", "server-state.capture.sanitized.json")),
        )

        val snapshot = parser.buildSnapshot(
            details = details,
            runtime = runtime,
            collectedAt = Instant.parse("2026-03-24T12:11:00Z"),
        )
        val metrics = snapshot.metrics.associateBy { it.key }

        assertEquals("11111111-2222-3333-4444-555555555555", snapshot.resourceId)
        assertEquals("VMVM", snapshot.displayName)
        assertEquals("running", metrics.getValue("state.power").value?.raw)
        assertEquals("false", metrics.getValue("state.suspended").value?.raw)
        assertEquals("2", metrics.getValue("quota.cpu_cores").value?.raw)
        assertEquals("2147483648", metrics.getValue("quota.memory_bytes").value?.raw)
        assertEquals("2029658112", metrics.getValue("usage.memory_total_bytes").value?.raw)
        assertEquals("282603520", metrics.getValue("usage.memory_used_bytes").value?.raw)
        assertEquals("1.0", metrics.getValue("state.cpu").value?.raw)
        assertEquals("56807424", metrics.getValue("state.memory.buffers").value?.raw)
        assertEquals("145014784", metrics.getValue("state.memory.cached").value?.raw)
        assertEquals("46895104", metrics.getValue("state.memory.sreclaimable").value?.raw)
        assertEquals("agent_meminfo", metrics.getValue("state.memory._source").value?.raw)
        assertEquals("42949672960", metrics.getValue("quota.disk_bytes").value?.raw)
        assertEquals("5559681024", metrics.getValue("usage.disk_used_bytes").value?.raw)
        assertEquals("21172646400", metrics.getValue("state.disk.vda.rd.bytes").value?.raw)
        assertEquals("15499561472", metrics.getValue("state.disk.vda.wr.bytes").value?.raw)
        assertEquals("5560053760", metrics.getValue("state.disk.vda.physical").value?.raw)
        assertEquals("536870912000", metrics.getValue("quota.traffic_bytes").value?.raw)
        assertEquals("5181442543", metrics.getValue("usage.traffic_total_bytes").value?.raw)
        assertEquals("true", metrics.getValue("state.traffic.total.sql").value?.raw)
        assertEquals("8135896", metrics.getValue("state.network.eth0.rx.pkts").value?.raw)
        assertEquals("580871", metrics.getValue("state.network.eth0.tx.pkts").value?.raw)
        assertEquals("2026-03-23T00:00:00.000000Z", metrics.getValue("meta.traffic_cycle_start_at").value?.raw)
        assertEquals("2026-04-22T23:59:59.999999Z", metrics.getValue("meta.traffic_cycle_end_at").value?.raw)
        assertEquals("Debian GNU/Linux 13 (trixie)", metrics.getValue("meta.os_name").value?.raw)
        assertEquals("203.0.113.170", metrics.getValue("ipv4").value?.raw)
        assertEquals("6.12.38+deb13-amd64", metrics.getValue("os.kernel").value?.raw)
        assertEquals("Asia/Tokyo", metrics.getValue("timezone").value?.raw)
        assertEquals("false", metrics.getValue("trafficExceeded").value?.raw)
        assertTrue(metrics.getValue("state.running").supported)
        assertFalse(metrics.getValue("usage.net_rx_bytes").value?.raw.isNullOrBlank())
    }

    private fun fixturePath(vararg parts: String): Path {
        val repoRoot = System.getProperty("nodestatus.repoRoot")
        require(!repoRoot.isNullOrBlank()) { "Missing nodestatus.repoRoot system property" }
        return Path.of(repoRoot, *parts)
    }
}
