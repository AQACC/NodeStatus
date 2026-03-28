package com.aqa.cc.nodestatus;

import android.app.Application;

public final class NodeStatusApp extends Application {
    private static volatile NodeStatusApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        new AppLocaleStore(this).applyStoredLocale();
    }

    public static Application appContext() {
        if (instance == null) {
            throw new IllegalStateException("NodeStatusApp is not initialized.");
        }
        return instance;
    }
}
