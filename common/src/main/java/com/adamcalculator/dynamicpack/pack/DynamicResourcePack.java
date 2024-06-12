package com.adamcalculator.dynamicpack.pack;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.sync.SyncProgress;
import com.adamcalculator.dynamicpack.sync.SyncBuilder;
import com.adamcalculator.dynamicpack.sync.SyncingTask;
import com.adamcalculator.dynamicpack.util.*;
import com.adamcalculator.dynamicpack.status.StatusChecker;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DynamicResourcePack extends AbstractPack {
    private final File location; // in resourcepack dir
    private final JSONObject cachedJson; // java-json of dynamicmcpack.json
    private final Remote remote; // serialized remote block
    private final String remoteTypeStr;

    private boolean cachedUpdateAvailable;
    private Exception latestException;
    private final List<Consumer<DynamicResourcePack>> destroyListeners = new ArrayList<>();
    private boolean isSyncing = false; // currently syncing
    private boolean destroyed = false; // destroyed


    public DynamicResourcePack(File location, JSONObject json) {
        this.location = location;
        this.cachedJson = json;

        try {
            JSONObject remote = json.getJSONObject("remote");
            this.remoteTypeStr = remote.getString("type");
            this.remote = Remote.REMOTES.get(this.remoteTypeStr).get();
            this.remote.init(this, remote);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse 'remote' block", e);
        }
    }

    /**
     * Return pack SyncBuilder with wrapped throws
     */
    public SyncBuilder syncBuilder() {
        return new SyncBuilder() {
            private SyncBuilder builder;

            private void wrapThrowable(ThrowingFunction<Exception> fun) {
                wrapThrowableRet(() -> {
                    fun.run();
                    return null;
                }, null);
            }

            private <T> T wrapThrowableRet(ThrowingFunctionRet<Exception, T> fun, T def) {
                try {
                    return fun.run();
                } catch (Exception e) {
                    isSyncing = false;
                    setLatestException(e);
                    Out.error("Pack " + getName() + " error while doUpdate (or init) SyncBuilder", e);
                }
                return def;
            }

            @Override
            public void init() {
                wrapThrowable(() -> {
                    checkNetwork();
                    builder = remote.syncBuilder();
                    builder.init();
                });
            }

            @Override
            public long getDownloadedSize() {
                return wrapThrowableRet(() -> builder.getDownloadedSize(), -1L);

            }

            @Override
            public boolean isUpdateAvailable() {
                return wrapThrowableRet(() -> builder.isUpdateAvailable(), false);
            }

            @Override
            public long getUpdateSize() {
                return wrapThrowableRet(() -> builder.getUpdateSize(), -1L);
            }

            @Override
            public boolean doUpdate(SyncProgress progress) {
                SyncingTask.currentPackName = getName();

                return wrapThrowableRet(() -> {
                    isSyncing = true;
                    boolean b = builder.doUpdate(progress);
                    try {
                        validateSafePackMinecraftMeta();
                        setLatestException(null);

                    } catch (Exception e2) {
                        Out.error("Error while check safe pack meta", e2);
                        setLatestException(e2);
                    }
                    isSyncing = false;
                    return b;
                }, false);
            }
        };
    }

    @Override
    public boolean isSyncing() {
        return isSyncing;
    }

    // See StatusChecker for this.
    // Developer can block network for specify version in dynamicpack.status.v1.json by security questions
    public boolean isNetworkBlocked() {
        return StatusChecker.isBlockUpdating(remoteTypeStr);
    }

    private void checkNetwork() {
        if (isNetworkBlocked()) {
            throw new SecurityException("Network is blocked for remote_type=" + remoteTypeStr + " current version of mod not safe. Update mod!");
        }
    }

    public boolean isZip() {
        if (location.isDirectory()) {
            return false;
        }
        return location.getName().toLowerCase().endsWith(".zip");
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public Remote getRemote() {
        return remote;
    }

    public File getLocation() {
        return location;
    }

    public String getName() {
        return location.getName();
    }

    public JSONObject getPackJson() {
        return cachedJson;
    }

    public JSONObject getCurrentJson() {
        return cachedJson.getJSONObject("current");
    }

    public String getRemoteType() {
        return remoteTypeStr;
    }

    public void setLatestException(Exception e) {
        Out.debug(this + ": latestException="+e);
        this.latestException = e;
    }

    public Exception getLatestException() {
        return latestException;
    }

    public long getLatestUpdated() {
        try {
            return cachedJson.getJSONObject("current").getLong("latest_updated");

        } catch (Exception e) {
            return -1;
        }
    }

    public void updateJsonLatestUpdate() {
        cachedJson.getJSONObject("current").put("latest_updated", System.currentTimeMillis() / 1000);
    }

    public boolean checkIsUpdateAvailable() throws IOException {
        checkNetwork();
        return cachedUpdateAvailable = remote.checkUpdateAvailable();
    }

    public boolean getCachedUpdateAvailableStatus() {
        return cachedUpdateAvailable;
    }

    private void validateSafePackMinecraftMeta() throws Exception {
        PackUtil.openPackFileSystem(location, path -> {
            Path mcmeta = path.resolve(SharedConstrains.MINECRAFT_META);
            boolean safe = PackUtil.isPathFileExists(mcmeta);
            if (safe) {
                try {
                    safe = checkMinecraftMetaIsValid(PackUtil.readString(mcmeta));
                } catch (IOException ignored) {
                    safe = false;
                }
            }
            if (!safe) {
                PathsUtil.nioWriteText(mcmeta, SharedConstrains.UNKNOWN_PACK_MCMETA);
            }
        });
    }

    private boolean checkMinecraftMetaIsValid(String s) {
        try {
            return DynamicPackMod.INSTANCE.checkResourcePackMetaValid(s);

        } catch (Exception e) {
            Out.error("Error while check meta valid.", e);
            return false;
        }
    }

    public void addDestroyListener(Consumer<DynamicResourcePack> runnable) {
        destroyListeners.add(runnable);
    }

    public void removeDestroyListener(Consumer<DynamicResourcePack> runnable) {
        destroyListeners.remove(runnable);
    }

    /**
     * Give your predecessor's successor to update some fields
     * <p>English?</p>
     * @param oldestPack predecessor
     */
    public void flashback(@Nullable DynamicResourcePack oldestPack) {
        if (oldestPack == null) return;
        oldestPack.markAsDestroyed(this);

        if (this.latestException == null) {
            this.latestException = oldestPack.latestException;
        }
    }

    private void markAsDestroyed(DynamicResourcePack heirPack) {
        for (Consumer<DynamicResourcePack> runnable : destroyListeners.toArray(new Consumer[0])) {
            runnable.accept(heirPack);
        }
        destroyListeners.clear();
        this.destroyed = true;
    }

}
