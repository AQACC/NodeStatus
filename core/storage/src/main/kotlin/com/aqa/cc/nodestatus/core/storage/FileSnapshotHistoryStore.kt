package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import kotlin.math.max

class FileSnapshotHistoryStore(
    private val path: java.nio.file.Path,
    private val maxSnapshotCount: Int = 240,
) {
    fun appendAll(snapshots: List<ResourceSnapshot>) {
        if (snapshots.isEmpty()) {
            return
        }
        val existing = SnapshotFileCodec.read(path)
        val merged = (existing + snapshots)
            .sortedBy { it.collectedAt }
            .takeLast(max(1, maxSnapshotCount))
        SnapshotFileCodec.write(path, merged)
    }

    fun listHistory(): List<ResourceSnapshot> =
        SnapshotFileCodec.read(path).sortedBy { it.collectedAt }

    fun listHistory(resourceId: String, limit: Int): List<ResourceSnapshot> =
        listHistory()
            .filter { it.resourceId == resourceId }
            .takeLast(limit)
}
