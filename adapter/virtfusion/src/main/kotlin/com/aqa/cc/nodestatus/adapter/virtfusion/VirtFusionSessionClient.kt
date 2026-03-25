package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.nio.file.Path
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import javax.net.ssl.SSLContext
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class VirtFusionCollectedServer(
    val listEntry: VirtFusionServerListEntry,
    val details: VirtFusionServerDetails,
    val runtime: VirtFusionRuntimeState,
    val snapshot: ResourceSnapshot,
)

data class VirtFusionHttpRequest(
    val uri: URI,
    val headers: Map<String, String>,
)

fun interface VirtFusionHttpTransport {
    fun get(request: VirtFusionHttpRequest): String
}

class UrlConnectionVirtFusionHttpTransport(
    auth: VirtFusionLocalSessionAuth? = null,
    private val sslContext: SSLContext? = buildSslContext(auth),
) : VirtFusionHttpTransport {
    override fun get(request: VirtFusionHttpRequest): String {
        val connection = URL(request.uri.toString()).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        request.headers.forEach { (name, value) ->
            connection.setRequestProperty(name, value)
        }

        if (connection is HttpsURLConnection && sslContext != null) {
            connection.sslSocketFactory = sslContext.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val body = stream?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                reader.readText()
            }
        }.orEmpty()

        require(statusCode in 200..299) {
            "Request failed with HTTP $statusCode for ${request.uri}"
        }
        return body
    }

    companion object {
        private fun buildSslContext(auth: VirtFusionLocalSessionAuth?): SSLContext? =
            if (auth?.allowInsecureTls == true) insecureSslContext() else null

        private fun insecureSslContext(): SSLContext {
            val trustAll = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                },
            )
            return SSLContext.getInstance("TLS").apply {
                init(null, trustAll, SecureRandom())
            }
        }
    }
}

class VirtFusionSessionClient(
    private val auth: VirtFusionLocalSessionAuth,
    private val parser: VirtFusionCapturedResponseParser = VirtFusionCapturedResponseParser(),
    private val transport: VirtFusionHttpTransport = UrlConnectionVirtFusionHttpTransport(auth),
) {
    fun fetchServerList(limit: Int = 8): List<VirtFusionServerListEntry> {
        val request = auth.newAuthorizedGet(
            uri = auth.serverListUri(limit),
            referer = auth.dashboardReferer(),
        )
        return parser.parseServerList(transport.get(request))
    }

    fun fetchServer(server: VirtFusionServerListEntry, collectedAt: Instant = Instant.now()): VirtFusionCollectedServer {
        val referer = auth.serverReferer(server.resourceId)
        val details = parser.parseServerDetails(
            transport.get(
                auth.newAuthorizedGet(
                    uri = auth.serverDetailsUri(requireNumericId(server)),
                    referer = referer,
                ),
            ),
        )
        val runtime = parser.parseRuntimeState(
            transport.get(
                auth.newAuthorizedGet(
                    uri = auth.serverStateUri(requireNumericId(server)),
                    referer = referer,
                ),
            ),
        )
        val snapshot = parser.buildSnapshot(details, runtime, collectedAt)

        return VirtFusionCollectedServer(
            listEntry = server,
            details = details,
            runtime = runtime,
            snapshot = snapshot,
        )
    }

    fun fetchSnapshots(limit: Int = 8, collectedAt: Instant = Instant.now()): List<ResourceSnapshot> =
        fetchServerList(limit).map { server ->
            fetchServer(server, collectedAt).snapshot
        }

    companion object {
        @JvmStatic
        fun create(auth: VirtFusionLocalSessionAuth): VirtFusionSessionClient =
            VirtFusionSessionClient(auth = auth)

        @JvmStatic
        fun fromLocalAuthFile(
            authPath: Path,
            parser: VirtFusionCapturedResponseParser = VirtFusionCapturedResponseParser(),
            transportFactory: (VirtFusionLocalSessionAuth) -> VirtFusionHttpTransport = { auth -> UrlConnectionVirtFusionHttpTransport(auth) },
        ): VirtFusionSessionClient {
            val auth = VirtFusionLocalSessionAuthLoader.load(authPath)
            return VirtFusionSessionClient(
                auth = auth,
                parser = parser,
                transport = transportFactory(auth),
            )
        }

        private fun requireNumericId(server: VirtFusionServerListEntry): Long =
            requireNotNull(server.resourceNumericId) {
                "VirtFusion server list entry is missing numeric server id for ${server.resourceId}"
            }
    }
}
