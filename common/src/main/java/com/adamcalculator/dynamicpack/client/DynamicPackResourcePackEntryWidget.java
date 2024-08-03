package com.adamcalculator.dynamicpack.client;

import com.adamcalculator.dynamicpack.DynamicPackMod;
import com.adamcalculator.dynamicpack.pack.DynamicResourcePack;
import io.gitlab.jfronny.libjf.entrywidgets.api.v0.ResourcePackEntryWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.resources.ResourceLocation;
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
            drawTexture(context, pack, x, y, hovered);
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
            openPackScreen(pack);
        }
    }

    private @Nullable DynamicResourcePack getDynamicPackFromArgs(PackSelectionModel.Entry entry) {
        return DynamicPackMod.getDynamicPackByMinecraftName(entry.getId());
    }

    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.tryBuild("dynamicpack", "select_button.png");
    private static final ResourceLocation BUTTON_WARNING_TEXTURE = ResourceLocation.tryBuild("dynamicpack", "select_button_warning.png");
    private static final ResourceLocation BUTTON_SYNCING_TEXTURE = ResourceLocation.tryBuild("dynamicpack", "select_button_syncing.png");

    public static void drawTexture(GuiGraphics context, DynamicResourcePack pack, int x, int y, boolean hovered) {
        Exception latestException = pack.getLatestException();
        if (pack.isSyncing()) {
            Compat.drawTexture(context, BUTTON_TEXTURE, x, y, 0.0F, (hovered ? 16f : 0f), 16, 16, 16, 32);


            double alpha = System.currentTimeMillis() / 200d;
            int xshift = (int) (Math.sin(alpha) * 6.9d);
            int yshift = (int) (Math.cos(alpha) * 6.9d);

            Compat.drawTexture(context, BUTTON_SYNCING_TEXTURE, x + xshift+6, y + yshift+6, 0.0F, (hovered ? 16f : 0f), 4, 4, 16, 32);

        } else if (latestException != null) {
            Compat.drawTexture(context, BUTTON_WARNING_TEXTURE, x, y, 0.0F, (hovered ? 16f : 0f), 16, 16, 16, 32);

        } else {
            Compat.drawTexture(context, BUTTON_TEXTURE, x, y, 0.0F, (hovered ? 16f : 0f), 16, 16, 16, 32);
        }
    }

    public static void openPackScreen(DynamicResourcePack pack) {
        Minecraft.getInstance().setScreen(new DynamicPackScreen(Minecraft.getInstance().screen, pack));
    }

}
