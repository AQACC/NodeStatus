package com.aqa.cc.nodestatus.adapter.virtfusion

import com.aqa.cc.nodestatus.adapter.engine.AuthMethod
import com.aqa.cc.nodestatus.adapter.engine.Capability
import com.aqa.cc.nodestatus.core.model.ProviderFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtFusionAdapterBlueprintTest {
    @Test
    fun manifest_stays_bound_to_virtfusion_family() {
        assertEquals(ProviderFamily.VIRT_FUSION, VirtFusionVmvmBlueprint.manifest.family)
        assertTrue(VirtFusionVmvmBlueprint.manifest.capabilities.contains(Capability.TRAFFIC_USAGE))
        assertTrue(VirtFusionVmvmBlueprint.manifest.supportedAuth.contains(AuthMethod.WEB_SESSION))
        assertFalse(VirtFusionVmvmBlueprint.manifest.supportedAuth.contains(AuthMethod.API_KEY))
    }
}

