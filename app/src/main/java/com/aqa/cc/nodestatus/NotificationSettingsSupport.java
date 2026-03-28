package com.aqa.cc.nodestatus;

import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.android.material.materialswitch.MaterialSwitch;

final class NotificationSettingsSupport {
    static final int DEFAULT_TRAFFIC_THRESHOLD_PERCENT = 20;
    private static final int MIN_TRAFFIC_THRESHOLD_PERCENT = 1;
    private static final int MAX_TRAFFIC_THRESHOLD_PERCENT = 100;

    private NotificationSettingsSupport() {
    }

    static int parseThresholdPercent(@NonNull EditText trafficThresholdInput) {
        String raw = trafficThresholdInput.getText().toString().trim();
        if (raw.isEmpty()) {
            return DEFAULT_TRAFFIC_THRESHOLD_PERCENT;
        }
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < MIN_TRAFFIC_THRESHOLD_PERCENT) {
                return MIN_TRAFFIC_THRESHOLD_PERCENT;
            }
            if (parsed > MAX_TRAFFIC_THRESHOLD_PERCENT) {
                return MAX_TRAFFIC_THRESHOLD_PERCENT;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return DEFAULT_TRAFFIC_THRESHOLD_PERCENT;
        }
    }

    static void bindNotificationControls(
            @NonNull View rootView,
            @NonNull MaterialSwitch notificationsEnabledSwitch,
            @NonNull EditText trafficThresholdInput
    ) {
        View thresholdLayout = rootView.findViewById(R.id.trafficThresholdLayout);
        View thresholdHelperText = rootView.findViewById(R.id.trafficThresholdHelperText);
        notificationsEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                applyNotificationState(thresholdLayout, thresholdHelperText, trafficThresholdInput, isChecked));
        applyNotificationState(
                thresholdLayout,
                thresholdHelperText,
                trafficThresholdInput,
                notificationsEnabledSwitch.isChecked()
        );
    }

    private static void applyNotificationState(
            View thresholdLayout,
            View thresholdHelperText,
            @NonNull EditText trafficThresholdInput,
            boolean enabled
    ) {
        trafficThresholdInput.setEnabled(enabled);
        if (thresholdLayout != null) {
            thresholdLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        if (thresholdHelperText != null) {
            thresholdHelperText.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }
}
