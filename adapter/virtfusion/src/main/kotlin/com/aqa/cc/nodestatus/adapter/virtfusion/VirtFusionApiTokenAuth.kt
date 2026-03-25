package com.aqa.cc.nodestatus.adapter.virtfusion

import java.net.URI

data class VirtFusionApiTokenAuth(
    val apiBaseUrl: String,
    val apiToken: String,
    val userAgent: String = VirtFusionLocalSessionAuth.DEFAULT_USER_AGENT,
    val allowInsecureTls: Boolean = false,
) {
    fun accountUri(): URI = URI.create("$apiBaseUrl/account")

    fun serversUri(results: Int = 8): URI = URI.create("$apiBaseUrl/server?results=$results")

    fun serverUri(serverId: String): URI = URI.create("$apiBaseUrl/server/$serverId")

    fun serverTasksUri(serverId: String): URI = URI.create("$apiBaseUrl/server/$serverId/tasks")

    fun newAuthorizedGet(uri: URI): VirtFusionHttpRequest =
        VirtFusionHttpRequest(
            uri = uri,
            headers = mapOf(
                "Accept" to "application/json",
                "Authorization" to "Bearer $apiToken",
                "User-Agent" to userAgent,
            ),
        )
}
