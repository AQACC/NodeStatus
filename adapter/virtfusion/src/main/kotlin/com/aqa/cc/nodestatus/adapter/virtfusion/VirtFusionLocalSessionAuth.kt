package com.aqa.cc.nodestatus.adapter.virtfusion

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class VirtFusionLocalSessionAuth(
    val baseUrl: String,
    val cookieHeader: String,
    val xsrfHeader: String,
    val userAgent: String = DEFAULT_USER_AGENT,
    val allowInsecureTls: Boolean = false,
) {
    fun serverListUri(limit: Int = 8): URI = URI.create("$baseUrl/servers/_list?limit=$limit")

    fun serverDetailsUri(serverId: Long): URI = URI.create("$baseUrl/server/$serverId/resource/server.json")

    fun serverStateUri(serverId: Long): URI = URI.create("$baseUrl/server/$serverId/resource/state.json")

    fun queueUri(serverId: Long, results: Int = 6, page: Int = 1): URI =
        URI.create("$baseUrl/queue/list?server_id=$serverId&results=$results&page=$page")

    fun dashboardReferer(): String = "$baseUrl/dashboard"

    fun serverReferer(serverUuid: String): String = "$baseUrl/server/$serverUuid"

    fun newAuthorizedGet(uri: URI, referer: String): VirtFusionHttpRequest =
        VirtFusionHttpRequest(
            uri = uri,
            headers = buildMap {
                put("Accept", "application/json, text/plain, */*")
                put("Cookie", cookieHeader)
                put("Referer", referer)
                put("User-Agent", userAgent)
                put("X-Requested-With", "XMLHttpRequest")
                if (xsrfHeader.isNotBlank()) {
                    put("X-XSRF-TOKEN", xsrfHeader)
                }
            },
        )

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0"
    }
}

object VirtFusionLocalSessionAuthLoader {
    fun load(path: Path): VirtFusionLocalSessionAuth {
        require(Files.exists(path)) { "Missing local auth file: $path" }

        val properties = Properties()
        Files.newBufferedReader(path).use(properties::load)

        return VirtFusionLocalSessionAuth(
            baseUrl = properties.requireValue("virtfusion.base_url"),
            cookieHeader = properties.requireValue("virtfusion.cookie_header"),
            xsrfHeader = properties.getSanitizedProperty("virtfusion.x_xsrf_token")?.trim().orEmpty(),
            userAgent = properties.getProperty("virtfusion.user_agent")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: VirtFusionLocalSessionAuth.DEFAULT_USER_AGENT,
            allowInsecureTls = properties.getProperty("virtfusion.allow_insecure_tls")
                ?.trim()
                ?.equals("true", ignoreCase = true)
                ?: false,
        )
    }

    fun defaultPath(repoRoot: Path): Path = repoRoot.resolve("local.auth.properties")

    private fun Properties.requireValue(key: String): String =
        getSanitizedProperty(key)?.trim()?.takeIf { it.isNotBlank() }
            ?: error("Missing required property '$key'")

    private fun Properties.getSanitizedProperty(key: String): String? {
        getProperty(key)?.let { return it }
        val bomKey = "\uFEFF$key"
        getProperty(bomKey)?.let { return it }
        return stringPropertyNames()
            .firstOrNull { it.trimStart('\uFEFF') == key }
            ?.let { getProperty(it) }
    }
}
