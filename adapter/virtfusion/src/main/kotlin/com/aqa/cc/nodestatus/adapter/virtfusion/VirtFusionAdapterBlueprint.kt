package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.adapter.engine.AuthMethod
import com.aqa.cc.nodestatus.adapter.engine.Capability
import com.aqa.cc.nodestatus.adapter.engine.PanelAdapterBlueprint
import com.aqa.cc.nodestatus.adapter.engine.RulePackManifest
import com.aqa.cc.nodestatus.core.model.ProviderFamily

object VirtFusionVmvmBlueprint : PanelAdapterBlueprint {
    override val manifest: RulePackManifest = RulePackManifest(
        id = "virtfusion.vmvm",
        family = ProviderFamily.VIRT_FUSION,
        displayName = "VirtFusion / vmvm",
        version = "0.1.0",
        supportedAuth = setOf(
            AuthMethod.API_KEY,
            AuthMethod.WEB_SESSION,
            AuthMethod.MANUAL_COOKIE,
        ),
        capabilities = setOf(
            Capability.INSTANCE_LIST,
            Capability.POWER_STATE,
            Capability.TRAFFIC_QUOTA,
            Capability.TRAFFIC_USAGE,
            Capability.CPU_USAGE,
            Capability.MEMORY_USAGE,
            Capability.DISK_USAGE,
        ),
        fixtureDirectory = "rule-packs/packs/virtfusion.vmvm/fixtures",
    )
}
