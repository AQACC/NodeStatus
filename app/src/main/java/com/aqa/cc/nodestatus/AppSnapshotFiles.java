package com.aqa.cc.nodestatus;

import android.content.Context;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppSnapshotFiles {
    private AppSnapshotFiles() {
    }

    public static Path latestSnapshotPath(Context context) {
        return Paths.get(context.getFilesDir().getAbsolutePath(), "latest-snapshots.properties");
    }

    public static Path historySnapshotPath(Context context) {
        return Paths.get(context.getFilesDir().getAbsolutePath(), "history-snapshots.properties");
    }

    public static Path debugDirectoryPath(Context context) {
        return Paths.get(context.getFilesDir().getAbsolutePath(), "debug");
    }

    public static Path virtFusionRefreshDebugDirectoryPath(Context context) {
        return debugDirectoryPath(context).resolve("virtfusion-refresh");
    }
}
