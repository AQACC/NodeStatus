package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.Freshness
import com.aqa.cc.nodestatus.core.model.Metric
import com.aqa.cc.nodestatus.core.model.MetricValue
import com.aqa.cc.nodestatus.core.model.MetricValueType
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import com.aqa.cc.nodestatus.core.model.ResourceKind
import com.aqa.cc.nodestatus.core.model.ResourceSnapshot
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.Properties

class FileSnapshotStore(
    private val path: Path,
) : SnapshotStore {
    override fun save(snapshot: ResourceSnapshot) {
        saveAll(listOf(snapshot))
    }

    override fun saveAll(snapshots: List<ResourceSnapshot>) {
        SnapshotFileCodec.write(path, snapshots)
    }

    override fun find(resourceId: String): ResourceSnapshot? =
        listLatest().firstOrNull { it.resourceId == resourceId }

    override fun listLatest(): List<ResourceSnapshot> {
        return SnapshotFileCodec.read(path)
    }
}
