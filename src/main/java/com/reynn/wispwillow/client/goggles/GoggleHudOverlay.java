package com.reynn.wispwillow.client.goggles;

import com.reynn.wispwillow.WispWillow;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * The panel that fades in beside the crosshair when a goggle-wearer looks at something
 * with a story to tell.
 */
@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class GoggleHudOverlay implements LayeredDraw.Layer {
    private GoggleHudOverlay() {}

    private static final int PAD = 5;
    private static final int LINE_H = 10;
    private static final int BAR_H = 3;

    private static final int BG = 0xD00E0A18;
    private static final int BORDER = 0x80B28BFF;
    private static final int TITLE = 0xFFD69BFF;
    private static final int BAR_BG = 0xFF241A33;

    /** Eased in/out so the panel does not pop. */
    private static float alpha;
    private static BlockPos lastTarget;

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "arcane_goggles"),
                new GoggleHudOverlay());
    }

    @Override
    public void render(GuiGraphics gg, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        GoggleInformation info = null;
        BlockPos target = null;

        if (ArcaneVision.active()) {
            target = ArcaneVision.lookedAtBlock();
            if (target != null) {
                BlockEntity be = mc.level.getBlockEntity(target);
                if (be instanceof GoggleInformation gi) info = gi;
            }
        }

        // Reset the fade when the player looks at something else, so the new panel
        // animates in rather than inheriting the old one's opacity.
        if (target == null || !target.equals(lastTarget)) {
            if (info != null) alpha = 0f;
            lastTarget = target;
        }

        float step = delta.getGameTimeDeltaPartialTick(false) * 0.25f;
        alpha = Mth.clamp(info != null ? alpha + step : alpha - step, 0f, 1f);
        if (alpha <= 0.01f || info == null) return;

        drawPanel(gg, mc.font, info, alpha);
    }

    private static void drawPanel(GuiGraphics gg, Font font, GoggleInformation info, float a) {
        Component title = info.goggleTitle();
        List<Component> lines = new ArrayList<>();
        info.addGoggleLines(lines);

        float bar = info.goggleBar();
        boolean hasBar = bar >= 0f;

        int width = font.width(title);
        for (Component line : lines) width = Math.max(width, font.width(line));
        width += PAD * 2;

        int height = PAD * 2 + LINE_H + lines.size() * LINE_H + (hasBar ? BAR_H + 3 : 0);

        // Sit just right and below the crosshair, clear of the hotbar.
        int x = gg.guiWidth() / 2 + 14;
        int y = gg.guiHeight() / 2 - height / 2;

        int bg = withAlpha(BG, a);
        gg.fill(x, y, x + width, y + height, bg);
        // A one-pixel arcane rim, so it reads as part of the lenses.
        int border = withAlpha(BORDER, a);
        gg.fill(x, y, x + width, y + 1, border);
        gg.fill(x, y + height - 1, x + width, y + height, border);
        gg.fill(x, y, x + 1, y + height, border);
        gg.fill(x + width - 1, y, x + width, y + height, border);

        int ty = y + PAD;
        gg.drawString(font, title, x + PAD, ty, withAlpha(TITLE, a), false);
        ty += LINE_H;

        if (hasBar) {
            int bx0 = x + PAD, bx1 = x + width - PAD;
            gg.fill(bx0, ty, bx1, ty + BAR_H, withAlpha(BAR_BG, a));
            int filled = Mth.floor((bx1 - bx0) * Mth.clamp(bar, 0f, 1f));
            if (filled > 0) {
                gg.fill(bx0, ty, bx0 + filled, ty + BAR_H, withAlpha(info.goggleBarColor(), a));
            }
            ty += BAR_H + 3;
        }

        for (Component line : lines) {
            gg.drawString(font, line, x + PAD, ty, withAlpha(0xFFFFFFFF, a), false);
            ty += LINE_H;
        }
    }

    private static int withAlpha(int argb, float mul) {
        int base = (argb >>> 24) & 0xFF;
        if (base == 0) base = 0xFF; // treat "no alpha given" as opaque
        int a = Mth.clamp(Math.round(base * mul), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
