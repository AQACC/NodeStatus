package com.aqa.cc.nodestatus.core.storage

import com.aqa.cc.nodestatus.core.model.ResourceSnapshot

interface SnapshotStore {
    fun save(snapshot: ResourceSnapshot)

    fun saveAll(snapshots: List<ResourceSnapshot>)

    fun find(resourceId: String): ResourceSnapshot?

    fun listLatest(): List<ResourceSnapshot>
}
