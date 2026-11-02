package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.blocks.fluxia.FluxiaSpitterBlockEntity;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A spitter holding a charge shows it as a glowing orb at the nozzle: brighter and larger
 * the fuller it is, pulsing gently, and flaring for a moment after it fires.
 *
 * <p>The old renderer emitted six End Rod particles every five ticks whether or not the
 * spitter had anywhere to send them; the beams themselves are now drawn by
 * {@link FluxArcRenderer} from a single packet per transfer.</p>
 */
public class FluxiaSpitterBER implements BlockEntityRenderer<FluxiaSpitterBlockEntity> {

    private static final int ORB_ARGB = 0xFF9FF0FF;

    public FluxiaSpitterBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(FluxiaSpitterBlockEntity be,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffers,
                       int packedLight,
                       int packedOverlay) {

        Level level = be.getLevel();
        if (level == null) return;

        IFluxiaStorage storage = be.getFluxStorage();
        if (storage == null || storage.capacity() <= 0) return;

        int amount = storage.amount();
        if (amount <= 0) return;

        float fill = Mth.clamp((float) amount / storage.capacity(), 0f, 1f);
        float time = level.getGameTime() + partialTicks;

        // Breathe slowly; the pulse is what makes an idle spitter look alive.
        float pulse = 0.85f + 0.15f * Mth.sin(time * 0.12f);
        float size = (0.16f + 0.20f * fill) * pulse;

        int argb = fade(ORB_ARGB, 0.45f + 0.55f * fill);

        OverlayShapes.sprite(
                poseStack, buffers,
                Minecraft.getInstance().gameRenderer.getMainCamera(),
                new Vec3(0.5, 0.55, 0.5), size, argb,
                WWRenderTypes.surface(OverlayShapes.BEAM_TEX));
    }

    private static int fade(int argb, float mul) {
        int a = Mth.clamp(Math.round(((argb >>> 24) & 0xFF) * mul), 0, 255);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
