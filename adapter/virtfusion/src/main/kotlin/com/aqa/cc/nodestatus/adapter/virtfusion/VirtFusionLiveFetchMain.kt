package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.core.storage.FileSnapshotStore
import java.nio.file.Path
import java.time.Instant

fun main() {
    val repoRoot = System.getProperty("nodestatus.repoRoot")
        ?.takeIf { it.isNotBlank() }
        ?.let(Path::of)
        ?: error("Missing nodestatus.repoRoot system property")

    val authPath = VirtFusionLocalSessionAuthLoader.defaultPath(repoRoot)
    val client = VirtFusionSessionClient.fromLocalAuthFile(authPath)
    val collectedAt = Instant.now()
    val snapshots = client.fetchSnapshots(collectedAt = collectedAt)
    val snapshotFile = repoRoot.resolve("captures").resolve("latest-snapshots.properties")
    FileSnapshotStore(snapshotFile).saveAll(snapshots)

    println("Fetched ${snapshots.size} VirtFusion snapshot(s) at $collectedAt")
    println("Stored latest snapshots at $snapshotFile")
    snapshots.forEach { snapshot ->
        val metrics = snapshot.metrics.associateBy { it.key }
        val power = metrics["state.power"]?.value?.raw ?: "unknown"
        val traffic = metrics["usage.traffic_total_bytes"]?.value?.raw ?: "n/a"
        val memoryUsed = metrics["usage.memory_used_bytes"]?.value?.raw ?: "n/a"
        val diskUsed = metrics["usage.disk_used_bytes"]?.value?.raw ?: "n/a"

        println(
            buildString {
                append(snapshot.displayName)
                append(" [")
                append(snapshot.resourceId)
                append("]")
                append(" power=")
                append(power)
                append(" traffic_total_bytes=")
                append(traffic)
                append(" memory_used_bytes=")
                append(memoryUsed)
                append(" disk_used_bytes=")
                append(diskUsed)
            },
        )
    }
}
