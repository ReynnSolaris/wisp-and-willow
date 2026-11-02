package com.reynn.wispwillow.fluxia.hud;

import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.registry.AttachmentRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = WispWillow.MODID, value = Dist.CLIENT)
public final class FluxiaHud {

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, p);
    }

    // your three layers
    private static final ResourceLocation TEX_BG    = rl("textures/gui/fluxia_background.png");
    private static final ResourceLocation TEX_FILL  = rl("textures/gui/fluxia_fill.png");
    private static final ResourceLocation TEX_FRAME = rl("textures/gui/fluxia_border.png");

    private static final int TEX_W = 48;    // your PNGs are 64x64
    private static final int TEX_H = 48;

    private static final float UI_SCALE = 2.25f; // make bigger/smaller here
    private static final int   MARGIN   = 8;     // distance from screen edges

    private static float smoothRatio = 0f;
    private static int   lastCap     = -1;

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        // draw above the XP bar so it stays on top of vanilla
//        event.registerAbove(
//                VanillaGuiLayers.EXPERIENCE_BAR,
//                rl("fluxia"),
//                FluxiaHud::renderFluxBar
//        );
    }



    private static void renderFluxBar(GuiGraphics gg, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return;

        var fx = mc.player.getData(AttachmentRegistry.FLUXIA.get());
        if (fx == null) return;

        int cap = Math.max(1, fx.capacity());
        int amt = Math.max(0, Math.min(fx.amount(), cap));
        float target = amt / (float)cap;

        if (cap != lastCap) { smoothRatio = target; lastCap = cap; }
        else { smoothRatio += (target - smoothRatio) * 0.15f; }

        // anchor bottom-right of screen
        int sw = gg.guiWidth();
        int sh = gg.guiHeight();

        int drawW = Math.round(TEX_W * UI_SCALE);
        int drawH = Math.round(TEX_H * UI_SCALE);

        int x = sw - MARGIN - drawW;
        int y = sh - MARGIN - drawH;

        // draw using matrix scale (keep pixels crisp)
        var pose = gg.pose();
        pose.pushPose();
        // scale first, then translate in "pre-scaled" units
        pose.scale(UI_SCALE, UI_SCALE, 1f);
        pose.translate(x / UI_SCALE, y / UI_SCALE, 0);

        // background
        gg.blit(TEX_BG, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        // fill (clip width based on smoothRatio)
        int fillW = Math.max(0, Math.min(TEX_W, Math.round(TEX_W * smoothRatio)));
        if (fillW > 0) gg.blit(TEX_FILL, 0, 0, 0, 0, fillW, TEX_H, TEX_W, TEX_H);

        // frame last
        gg.blit(TEX_FRAME, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        // (optional) numbers next to bar while sneaking
        // String txt = amt + " / " + cap;
        // gg.drawString(gg.gui().getFont(), txt, -gg.gui().getFont().width(txt) - 4, TEX_H/2 - 4, 0xFFF3F6FF, true);

        pose.popPose();
    }
}