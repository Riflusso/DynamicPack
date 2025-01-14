package com.adamcalculator.dynamicpack.sync;

import com.adamcalculator.dynamicpack.Config;
import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import com.adamcalculator.dynamicpack.util.Out;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Sync task.
 * Re-check all packs and update packs with update available
 */
public class SyncingTask {
    private static boolean isSyncing = false;
    @NotNull private static String syncingLog1 = "";
    @NotNull private static String syncingLog2 = "";
    @NotNull private static String syncingLog3 = "";
    public static long eta;
    public static SyncBuilder currentRootSyncBuilder; // used in future for interrupt() download. Sets by Thread
    @Nullable public static String currentPackName;


    /**
     * @param runnable to run
     * @throws RuntimeException if task already running (check isSyncing before call)
     */
    public static void launchTaskAsSyncing(Runnable runnable) {
        if (isSyncing()) {
            throw new RuntimeException("Failed to launchTaskAsSyncing. Other task currently working...");
        }

        var packs = DynamicPackMod.getPacksContainer();
        setSyncing(true);
        packs.lockRescan();

        log("[SyncingTask] launchTaskAsSyncing start!");
        runnable.run();
        log("[SyncingTask] launchTaskAsSyncing end!");

        setSyncing(false);
        packs.unlockRescan();
        SyncingTask.currentPackName = null;
        SyncingTask.currentRootSyncBuilder = null;
    }

    public static void log(String s) {
        Out.debug("[SyncingTask] log: " + s);
        syncingLog1 = syncingLog2;
        syncingLog2 = syncingLog3;
        syncingLog3 = s;
    }

    public static void clearLog() {
        syncingLog3 = "";
        syncingLog2 = "";
        syncingLog1 = "";
    }

    public static String getLogs() {
        return syncingLog1 + "\n" + syncingLog2 + "\n" + syncingLog3;
    }

    public static void setSyncing(boolean isSyncing) {
        SyncingTask.isSyncing = isSyncing;
    }

    public static boolean isSyncing() {
        return isSyncing;
    }

    private SyncingTask() {
    }


    public static SyncBuilder rootSyncBuilder() {
        return new SyncBuilder() {
            private final Set<SyncBuilder> builders = new HashSet<>();
            private boolean updateAvailable;
            private long totalSize;
            private boolean interrupted;


            @Override
            public void init(boolean ignoreCaches) throws Exception {
                clearLog();

                for (DynamicResourcePack pack : DynamicPackMod.getPacksContainer().getPacks()) {
                    if (interrupted) {
                        return;
                    }

                    if (Config.getInstance().isUpdateOnlyEnabledPacks()) {
                        boolean enabled = DynamicPackMod.isResourcePackActive(pack);
                        if (!enabled) {
                            continue;
                        }
                    }
                    var builder = pack.syncBuilder();
                    builder.init(ignoreCaches);
                    builders.add(builder);

                    totalSize += builder.getUpdateSize();
                    if (builder.isUpdateAvailable()) {
                        updateAvailable = true;
                    }
                }

                Out.debug("[SyncingTask] rootSyncBuilder() totalSize=" + totalSize + " updateAvailable=" + updateAvailable);

            }

            @Override
            public boolean isUpdateAvailable() {
                return updateAvailable;
            }

            @Override
            public long getUpdateSize() {
                return totalSize;
            }

            @Override
            public long getDownloadedSize() {
                long updated = 0;
                for (SyncBuilder syncBuilder : builders) {
                    updated += syncBuilder.getDownloadedSize();
                }
                return updated;
            }

            @Override
            public boolean doUpdate(SyncProgress progress) throws Exception {
                boolean reload = false;
                for (SyncBuilder syncBuilder : builders) {
                    if (interrupted) {
                        return false;
                    }

                    if (syncBuilder.isUpdateAvailable()) {
                        boolean rel = syncBuilder.doUpdate(progress);
                        if (rel) {
                            reload = true;
                        }
                    }
                }
                return reload;
            }

            @Override
            public void interrupt() {
                interrupted = true;
                for (SyncBuilder builder : builders) {
                    builder.interrupt();
                }
            }
        };
    }
}
