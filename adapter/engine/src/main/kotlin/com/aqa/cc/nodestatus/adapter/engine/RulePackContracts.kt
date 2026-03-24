package com.aqa.cc.nodestatus.adapter.engine

import com.aqa.cc.nodestatus.core.model.ProviderFamily

enum class AuthMethod {
    API_KEY,
    WEB_SESSION,
    MANUAL_COOKIE,
}

enum class Capability {
    INSTANCE_LIST,
    POWER_STATE,
    TRAFFIC_QUOTA,
    TRAFFIC_USAGE,
    CPU_USAGE,
    MEMORY_USAGE,
    DISK_USAGE,
}

data class RulePackManifest(
    val id: String,
    val family: ProviderFamily,
    val displayName: String,
    val version: String,
    val supportedAuth: Set<AuthMethod>,
    val capabilities: Set<Capability>,
    val fixtureDirectory: String,
)

interface PanelAdapterBlueprint {
    val manifest: RulePackManifest
}
