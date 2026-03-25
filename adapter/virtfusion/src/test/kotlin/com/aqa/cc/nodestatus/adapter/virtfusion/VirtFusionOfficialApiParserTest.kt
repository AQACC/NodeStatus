package com.aqa.cc.nodestatus.adapter.virtfusion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class VirtFusionOfficialApiParserTest {
    private val parser = VirtFusionOfficialApiParser()

    @Test
    fun parses_server_list_and_detail_from_official_api_shape() {
        val listRaw = """
            {"current_page":1,"data":[{"id":"server-uuid","name":"VMVM","hostname":null,"suspended":false,"protected":false,"migrating":false,"deleting":false,"backupCreating":false,"rescue":false,"vncEnabled":false,"isoMounted":false,"uefi":false,"bootOrder":["hd","cdrom"],"memory":"2048 MB","cpu":"2 Core","storage":[{"capacity":"40 GB","enabled":true,"primary":true,"created":"2026-03-23T02:09:14+00:00"}],"network":{"primary":{"mac":"00:B7:FE:6A:64:40","limit":"500 GB","ipv4":[{"address":"203.0.113.170","gateway":"203.0.113.254","netmask":"255.255.255.128"}],"ipv6":[]},"secondary":[]},"currentMonthlyPeriod":{"start":"2026-03-23T00:00:00.000000Z","end":"2026-04-22T23:59:59.999999Z"},"created":"2026-03-23T02:09:14+00:00"}]}
        """.trimIndent()
        val detailRaw = """
            {"data":{"id":"server-uuid","name":"VMVM","hostname":null,"suspended":false,"protected":false,"migrating":false,"deleting":false,"backupCreating":false,"rescue":false,"vncEnabled":false,"isoMounted":false,"uefi":false,"bootOrder":["hd","cdrom"],"memory":"2048 MB","cpu":"2 Core","storage":[{"capacity":"40 GB","enabled":true,"primary":true,"created":"2026-03-23T02:09:14+00:00"}],"network":{"primary":{"mac":"00:B7:FE:6A:64:40","limit":"500 GB","ipv4":[{"address":"203.0.113.170","gateway":"203.0.113.254","netmask":"255.255.255.128"}],"ipv6":[]},"secondary":[]},"currentMonthlyPeriod":{"start":"2026-03-23T00:00:00.000000Z","end":"2026-04-22T23:59:59.999999Z"},"state":null,"created":"2026-03-23T02:09:14+00:00"}}
        """.trimIndent()

        val listEntry = parser.parseServerList(listRaw).single()
        val detail = parser.parseServerDetail(detailRaw)
        val snapshot = parser.buildSnapshot(detail, Instant.parse("2026-03-25T02:30:00Z"))
        val metrics = snapshot.metrics.associateBy { it.key }

        assertEquals("server-uuid", listEntry.resourceId)
        assertEquals(2147483648L, listEntry.quotaMemoryBytes)
        assertEquals(536870912000L, listEntry.quotaTrafficBytes)
        assertEquals("203.0.113.170", listEntry.primaryIpv4Address)
        assertEquals("VMVM", snapshot.displayName)
        assertEquals("2147483648", metrics.getValue("quota.memory_bytes").value?.raw)
        assertEquals("536870912000", metrics.getValue("quota.traffic_bytes").value?.raw)
        assertTrue(metrics.getValue("state.suspended").supported)
        assertFalse(metrics.getValue("state.power").supported)
    }
}
