package com.aqa.cc.nodestatus;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

final class AppLocaleStore {
    static final String LANGUAGE_SYSTEM = "system";
    static final String LANGUAGE_ZH_CN = "zh-CN";
    static final String LANGUAGE_EN = "en";

    private static final String PREFS_NAME = "app_locale";
    private static final String KEY_LANGUAGE_TAG = "language_tag";

    private final SharedPreferences preferences;

    AppLocaleStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    String loadLanguageTag() {
        String stored = preferences.getString(KEY_LANGUAGE_TAG, LANGUAGE_SYSTEM);
        if (stored == null || stored.isBlank()) {
            return LANGUAGE_SYSTEM;
        }
        return stored;
    }

    void applyStoredLocale() {
        applyLocales(loadLanguageTag(), false);
    }

    void applyLanguageTag(@NonNull String languageTag) {
        applyLocales(languageTag, true);
    }

    private void applyLocales(@NonNull String languageTag, boolean persist) {
        if (persist) {
            preferences.edit().putString(KEY_LANGUAGE_TAG, languageTag).apply();
        }
        AppCompatDelegate.setApplicationLocales(toLocaleList(languageTag));
    }

    private LocaleListCompat toLocaleList(String languageTag) {
        if (LANGUAGE_SYSTEM.equals(languageTag)) {
            return LocaleListCompat.getEmptyLocaleList();
        }
        return LocaleListCompat.forLanguageTags(languageTag);
    }
}
