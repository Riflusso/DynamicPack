package com.adamcalculator.dynamicpack.client.fabric;

import com.adamcalculator.dynamicpack.SharedConstrains;
import com.adamcalculator.dynamicpack.client.DynamicPackModBase;
import com.adamcalculator.dynamicpack.util.Loader;
import com.adamcalculator.dynamicpack.util.Out;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.metadata.CustomValue;

/**
 * Fabric impl for DynamicPack mod
 */
public class FabricDynamicPreLaunch extends DynamicPackModBase implements PreLaunchEntrypoint {
    private static FabricDynamicPreLaunch instance;

    @Override
    public void onPreLaunch() {
        instance = this;
        Out.println("DynamicPack loaded. Hello fabric world!");

        try {
            CustomValue customValue = FabricLoader.getInstance().getModContainer(SharedConstrains.MOD_ID).get().getMetadata().getCustomValue("dynamicpack:build_git_hash");
            String asString = customValue.getAsString();
            Out.println("Build git hash: " + asString);

        } catch (Exception ignored) {}

        var gameDir = FabricLoader.getInstance().getGameDir().toFile();
        init(gameDir, Loader.FABRIC);
    }

    public static FabricDynamicPreLaunch getInstance() {
        return instance;
    }

    @Override
    public boolean isModExists(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
