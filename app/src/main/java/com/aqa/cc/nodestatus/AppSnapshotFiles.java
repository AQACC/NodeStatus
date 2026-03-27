package com.aqa.cc.nodestatus;

import android.content.Context;

import java.nio.file.Path;

public final class AppSnapshotFiles {
    private AppSnapshotFiles() {
    }

    public static Path latestSnapshotPath(Context context) {
        return Path.of(context.getFilesDir().getAbsolutePath(), "latest-snapshots.properties");
    }

    public static Path historySnapshotPath(Context context) {
        return Path.of(context.getFilesDir().getAbsolutePath(), "history-snapshots.properties");
    }

    public static Path debugDirectoryPath(Context context) {
        return Path.of(context.getFilesDir().getAbsolutePath(), "debug");
    }

    public static Path virtFusionRefreshDebugDirectoryPath(Context context) {
        return debugDirectoryPath(context).resolve("virtfusion-refresh");
    }
}
