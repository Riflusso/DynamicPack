package com.adamcalculator.dynamicpack.client;

import com.adamcalculator.dynamicpack.DynamicPackMod;

public class PackMixinHelper {
    public static void minecraftInitReturned() {
        DynamicPackMod.getInstance().minecraftInitialized();
    }

    public static void updatePacksMinecraftRequest() {
        DynamicPackMod.getPacksContainer().rescan();
    }
}
