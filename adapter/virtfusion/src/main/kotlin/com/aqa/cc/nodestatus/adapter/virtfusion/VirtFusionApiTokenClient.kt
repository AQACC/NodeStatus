package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import java.time.Instant

class VirtFusionApiTokenClient(
    private val auth: VirtFusionApiTokenAuth,
    private val transport: VirtFusionHttpTransport = UrlConnectionVirtFusionHttpTransport(
        VirtFusionLocalSessionAuth(
            baseUrl = auth.apiBaseUrl,
            cookieHeader = "",
            xsrfHeader = "",
            userAgent = auth.userAgent,
            allowInsecureTls = auth.allowInsecureTls,
        ),
    ),
    private val parser: VirtFusionOfficialApiParser = VirtFusionOfficialApiParser(),
) {
    fun probeConnection(): String =
        transport.get(auth.newAuthorizedGet(auth.accountUri()))

    fun fetchServersRaw(results: Int = 8): String =
        transport.get(auth.newAuthorizedGet(auth.serversUri(results)))

    fun fetchServerRaw(serverId: String): String =
        transport.get(auth.newAuthorizedGet(auth.serverUri(serverId)))

    fun fetchServerTasksRaw(serverId: String): String =
        transport.get(auth.newAuthorizedGet(auth.serverTasksUri(serverId)))

    fun fetchServerList(results: Int = 8): List<VirtFusionApiServerListEntry> =
        parser.parseServerList(fetchServersRaw(results))

    fun fetchServerDetail(serverId: String): VirtFusionApiServerDetail =
        parser.parseServerDetail(fetchServerRaw(serverId))

    fun fetchSnapshots(results: Int = 8, collectedAt: Instant = Instant.now()): List<ResourceSnapshot> =
        fetchServerList(results).map { entry ->
            parser.buildSnapshot(
                detail = fetchServerDetail(entry.resourceId),
                collectedAt = collectedAt,
            )
        }

    companion object {
        @JvmStatic
        fun create(auth: VirtFusionApiTokenAuth): VirtFusionApiTokenClient =
            VirtFusionApiTokenClient(auth = auth)
    }
}
