package com.aqa.cc.nodestatus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;

final class SelectionChipFactory {
    private SelectionChipFactory() {
    }

    @NonNull
    static Chip createSelectableChip(
            @NonNull AppCompatActivity activity,
            @NonNull Object tag,
            @NonNull CharSequence text
    ) {
        Chip chip = new Chip(activity);
        chip.setTag(tag);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setChipIconVisible(false);
        chip.setTextStartPadding(dp(activity, 6));
        chip.setTextEndPadding(dp(activity, 6));
        chip.setChipMinHeight(dp(activity, 40));
        chip.setTextAppearanceResource(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        chip.setChipStrokeWidth(dp(activity, 1));
        chip.setChipBackgroundColorResource(R.color.chip_state_server_selector);
        chip.setChipStrokeColorResource(R.color.chip_state_server_selector_stroke);
        chip.setTextColor(activity.getColorStateList(R.color.chip_state_server_selector_text));
        return chip;
    }

    private static int dp(@NonNull AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
