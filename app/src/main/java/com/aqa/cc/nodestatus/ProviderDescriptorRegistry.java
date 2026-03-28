package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;

import java.util.List;

interface ProviderDescriptorRegistry {
    @NonNull
    ProviderDescriptor getDescriptor(@NonNull ProviderFamily providerFamily);

    @NonNull
    List<ProviderDescriptor> allDescriptors();
}
