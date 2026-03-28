package com.aqa.cc.nodestatus;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

final class LanguageButtonController {
    private static final String[] LANGUAGE_CYCLE = {
            AppLocaleStore.LANGUAGE_SYSTEM,
            AppLocaleStore.LANGUAGE_ZH_CN,
            AppLocaleStore.LANGUAGE_EN,
    };

    private final AppCompatActivity activity;
    private final AppLocaleStore localeStore;
    private final Runnable beforeLanguageChange;
    private final MaterialButton languageButton;

    LanguageButtonController(
            AppCompatActivity activity,
            AppLocaleStore localeStore,
            Runnable beforeLanguageChange
    ) {
        this.activity = activity;
        this.localeStore = localeStore;
        this.beforeLanguageChange = beforeLanguageChange;
        this.languageButton = activity.findViewById(R.id.languageButton);
    }

    void bind() {
        languageButton.setOnClickListener(view -> cycleLanguage());
    }

    void render() {
        languageButton.setText(resolveLanguageLabel(localeStore.loadLanguageTag()));
    }

    private void cycleLanguage() {
        String currentLanguageTag = localeStore.loadLanguageTag();
        int currentIndex = 0;
        for (int index = 0; index < LANGUAGE_CYCLE.length; index++) {
            if (LANGUAGE_CYCLE[index].equals(currentLanguageTag)) {
                currentIndex = index;
                break;
            }
        }
        String nextLanguageTag = LANGUAGE_CYCLE[(currentIndex + 1) % LANGUAGE_CYCLE.length];
        if (nextLanguageTag.equals(currentLanguageTag)) {
            return;
        }
        beforeLanguageChange.run();
        localeStore.applyLanguageTag(nextLanguageTag);
    }

    private CharSequence resolveLanguageLabel(String languageTag) {
        if (AppLocaleStore.LANGUAGE_ZH_CN.equals(languageTag)) {
            return activity.getString(R.string.language_chinese_short);
        }
        if (AppLocaleStore.LANGUAGE_EN.equals(languageTag)) {
            return activity.getString(R.string.language_english_short);
        }
        return activity.getString(R.string.language_system_short);
    }
}
