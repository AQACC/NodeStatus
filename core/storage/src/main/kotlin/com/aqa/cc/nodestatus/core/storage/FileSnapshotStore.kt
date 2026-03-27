package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot

class FileSnapshotStore(
    private val path: java.nio.file.Path,
) : SnapshotStore {
    override fun save(snapshot: ResourceSnapshot) {
        saveAll(listOf(snapshot))
    }

    override fun saveAll(snapshots: List<ResourceSnapshot>) {
        SnapshotFileCodec.write(path, snapshots)
    }

    override fun find(resourceId: String): ResourceSnapshot? =
        listLatest().firstOrNull { matchesRequestedResource(it, resourceId) }

    override fun listLatest(): List<ResourceSnapshot> {
        return SnapshotFileCodec.read(path)
    }
}

private fun matchesRequestedResource(snapshot: ResourceSnapshot, requestedResourceId: String): Boolean {
    if (snapshot.scopedResourceId == requestedResourceId || snapshot.resourceId == requestedResourceId) {
        return true
    }
    return snapshot.siteId.isBlank() && requestedResourceId.endsWith("::${snapshot.resourceId}")
}
