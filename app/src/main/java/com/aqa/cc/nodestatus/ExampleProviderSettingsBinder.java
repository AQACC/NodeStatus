package com.aqa.cc.nodestatus;

import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.aqa.cc.nodestatus.core.model.ProviderFamily;
import com.google.android.material.materialswitch.MaterialSwitch;

final class ExampleProviderSettingsBinder implements ProviderSettingsBinder {
    @Override
    public @NonNull ProviderFamily providerFamily() {
        return ProviderFamily.EXAMPLE;
    }

    @Override
    public int layoutResId() {
        return R.layout.provider_settings_example;
    }

    @Override
    public void bind(@NonNull View rootView, @NonNull ProviderSessionConfig config, boolean debugBuild) {
        MaterialSwitch notificationsEnabledSwitch = rootView.findViewById(R.id.notificationsEnabledSwitch);
        EditText trafficThresholdInput = rootView.findViewById(R.id.trafficThresholdInput);
        notificationsEnabledSwitch.setChecked(config.isNotificationsEnabled());
        trafficThresholdInput.setText(String.valueOf(config.getLowTrafficThresholdPercent()));
        NotificationSettingsSupport.bindNotificationControls(rootView, notificationsEnabledSwitch, trafficThresholdInput);
    }

    @Override
    public @NonNull ProviderSessionConfig read(@NonNull View rootView, boolean debugBuild) {
        MaterialSwitch notificationsEnabledSwitch = rootView.findViewById(R.id.notificationsEnabledSwitch);
        EditText trafficThresholdInput = rootView.findViewById(R.id.trafficThresholdInput);
        return new ProviderSessionConfig(
                "",
                "",
                "",
                "",
                ProviderSessionConfigStore.DEFAULT_USER_AGENT,
                false,
                notificationsEnabledSwitch.isChecked(),
                NotificationSettingsSupport.parseThresholdPercent(trafficThresholdInput)
        );
    }
}
