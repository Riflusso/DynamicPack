package com.adamcalculator.dynamicpack.client;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import io.gitlab.jfronny.libjf.entrywidgets.api.v0.ResourcePackEntryWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.jetbrains.annotations.Nullable;

public class DynamicPackResourcePackEntryWidget implements ResourcePackEntryWidget {
    @Override
    public boolean isVisible(PackSelectionModel.Entry pack, boolean selectable) {
        return getDynamicPackFromArgs(pack) != null;
    }

    @Override
    public void render(PackSelectionModel.Entry entry1, GuiGraphics context, int x, int y, boolean hovered, float tickDelta) {
        DynamicResourcePack pack = getDynamicPackFromArgs(entry1);

        if (pack != null) {
            PackMixinHelper.drawTexture(context, pack, x, y, hovered);
        }
    }

    @Override
    public int getWidth(PackSelectionModel.Entry pack) {
        return 16;
    }

    @Override
    public int getHeight(PackSelectionModel.Entry pack, int rowHeight) {
        return 16;
    }

    @Override
    public int getY(PackSelectionModel.Entry pack, int rowHeight) {
        return 16;
    }

    @Override
    public int getXMargin(PackSelectionModel.Entry pack) {
        return 2;
    }

    @Override
    public void onClick(PackSelectionModel.Entry entry) {
        DynamicResourcePack pack = getDynamicPackFromArgs(entry);
        if (pack != null) {
            PackMixinHelper.openPackScreen(pack);
        }
    }

    private @Nullable DynamicResourcePack getDynamicPackFromArgs(PackSelectionModel.Entry entry) {
        return DynamicPackMod.getDynamicPackByMinecraftName(entry.getId());
    }
}
