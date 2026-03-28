package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class AppProviderDescriptorRegistry implements ProviderDescriptorRegistry {
    private final Map<ProviderFamily, ProviderDescriptor> descriptors;
    private final List<ProviderDescriptor> allDescriptors;

    AppProviderDescriptorRegistry() {
        this(List.of(
                ProviderDescriptor.refreshable(
                        ProviderFamily.VIRT_FUSION,
                        R.string.provider_family_virtfusion,
                        R.string.provider_add_helper_virtfusion,
                        new VirtFusionSettingsBinder(),
                        new VirtFusionProviderRefreshAdapter()
                ),
                ProviderDescriptor.settingsOnly(
                        ProviderFamily.EXAMPLE,
                        R.string.provider_family_example,
                        R.string.provider_add_helper_example,
                        new ExampleProviderSettingsBinder()
                )
        ));
    }

    AppProviderDescriptorRegistry(@NonNull List<ProviderDescriptor> descriptors) {
        Map<ProviderFamily, ProviderDescriptor> descriptorMap = new EnumMap<>(ProviderFamily.class);
        for (ProviderDescriptor descriptor : descriptors) {
            descriptorMap.put(descriptor.providerFamily(), descriptor);
        }
        this.descriptors = descriptorMap;
        this.allDescriptors = descriptors;
    }

    @Override
    public @NonNull ProviderDescriptor getDescriptor(@NonNull ProviderFamily providerFamily) {
        ProviderDescriptor descriptor = descriptors.get(providerFamily);
        if (descriptor == null) {
            throw new IllegalStateException("No provider descriptor is registered for provider family " + providerFamily);
        }
        return descriptor;
    }

    @Override
    public @NonNull List<ProviderDescriptor> allDescriptors() {
        return allDescriptors;
    }
}
