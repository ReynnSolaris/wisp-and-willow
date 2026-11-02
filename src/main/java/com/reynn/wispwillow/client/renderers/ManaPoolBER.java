package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reynn.wispwillow.blocks.ManaPoolBlockEntity;
import com.reynn.wispwillow.fluxia.IFluxiaStorage;
import com.reynn.wispwillow.registry.FluidRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Renders the Fluxia surface inside a pool.
 *
 * <p>The level is interpolated toward its true value rather than snapped to nine steps,
 * the surface ripples, and the whole thing brightens as it fills, so a full pool reads at
 * a glance from across a base.</p>
 */
public class ManaPoolBER implements BlockEntityRenderer<ManaPoolBlockEntity> {

    /** Inner bowl of the model, in block units. */
    private static final float INSET = 2.0f / 16.0f;
    private static final float POOL_BOTTOM = 1.0f / 16.0f;
    private static final float POOL_TOP = 7.0f / 16.0f;

    /** Subdivision of the surface quad. More segments = smoother waves, more vertices. */
    private static final int SEGMENTS = 4;

    /** How fast the displayed level chases the real one. */
    private static final float LERP_RATE = 0.12f;

    /** Displayed fill per pool, so the surface glides instead of jumping on each packet. */
    private static final Map<ManaPoolBlockEntity, float[]> DISPLAY_FILL = new WeakHashMap<>();

    public ManaPoolBER(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public void render(@NotNull ManaPoolBlockEntity be,
                       float partialTicks,
                       @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource buffer,
                       int packedLight,
                       int packedOverlay) {

        Level level = be.getLevel();
        if (level == null) return;

        IFluxiaStorage stor = be.getFluxStorage();
        if (stor == null || stor.capacity() <= 0) return;

        float target = Mth.clamp((float) stor.amount() / stor.capacity(), 0f, 1f);
        float fill = smoothed(be, target);
        if (fill <= 0.001f) return;

        FluidStack fluidStack = new FluidStack(FluidRegistry.FLUXIA_FLUID.get(), 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTex = ext.getStillTexture(fluidStack);
        if (stillTex == null) return;

        BlockPos pos = be.getBlockPos();
        FluidState fluidState = fluidStack.getFluid().defaultFluidState();
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTex);

        int tint = ext.getTintColor(fluidState, level, pos);

        // A nearly-full pool glows; an almost-empty one is dim and cold.
        int light = blendLight(packedLight, fill);

        float surfaceY = POOL_BOTTOM + (POOL_TOP - POOL_BOTTOM) * fill;
        float time = level.getGameTime() + partialTicks;
        // Ripple amplitude shrinks as the pool fills, so a brimming pool sits still.
        float waveAmp = (0.35f / 16.0f) * (1.0f - fill * 0.75f);

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        drawSurface(vc, poseStack, sprite, surfaceY, waveAmp, time, light, tint);
        drawWalls(vc, poseStack, sprite, surfaceY, light, tint);
    }

    // ------------------------------------------------------------------

    private static float smoothed(ManaPoolBlockEntity be, float target) {
        float[] slot = DISPLAY_FILL.computeIfAbsent(be, k -> new float[]{target});
        // Snap on a big jump (chunk load, a tablet dumped in) rather than crawling to it.
        if (Math.abs(target - slot[0]) > 0.35f) slot[0] = target;
        else slot[0] += (target - slot[0]) * LERP_RATE;
        return Mth.clamp(slot[0], 0f, 1f);
    }

    /** Fluxia is self-lit; lerp the block light up toward full as the pool fills. */
    private static int blendLight(int packedLight, float fill) {
        int block = LightTexture.block(packedLight);
        int sky = LightTexture.sky(packedLight);
        int glow = Math.round(Mth.lerp(fill, block, 14f));
        return LightTexture.pack(Math.max(block, glow), sky);
    }

    /** Height of the ripple at a point on the surface. */
    private static float wave(float u, float v, float time, float amp) {
        if (amp <= 0f) return 0f;
        return amp * (Mth.sin(u * 6.0f + time * 0.09f) + Mth.cos(v * 5.0f + time * 0.07f)) * 0.5f;
    }

    private void drawSurface(VertexConsumer vc, PoseStack ps, TextureAtlasSprite sprite,
                             float surfaceY, float amp, float time, int light, int tint) {
        float min = INSET, max = 1.0f - INSET;
        float span = max - min;
        float step = span / SEGMENTS;

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        for (int ix = 0; ix < SEGMENTS; ix++) {
            for (int iz = 0; iz < SEGMENTS; iz++) {
                float x0 = min + ix * step, x1 = x0 + step;
                float z0 = min + iz * step, z1 = z0 + step;

                float tu0 = Mth.lerp((x0 - min) / span, u0, u1);
                float tu1 = Mth.lerp((x1 - min) / span, u0, u1);
                float tv0 = Mth.lerp((z0 - min) / span, v0, v1);
                float tv1 = Mth.lerp((z1 - min) / span, v0, v1);

                float y00 = surfaceY + wave(x0, z0, time, amp);
                float y10 = surfaceY + wave(x1, z0, time, amp);
                float y11 = surfaceY + wave(x1, z1, time, amp);
                float y01 = surfaceY + wave(x0, z1, time, amp);

                vertex(vc, ps, x0, y00, z0, tu0, tv0, light, tint, 0, 1, 0);
                vertex(vc, ps, x0, y01, z1, tu0, tv1, light, tint, 0, 1, 0);
                vertex(vc, ps, x1, y11, z1, tu1, tv1, light, tint, 0, 1, 0);
                vertex(vc, ps, x1, y10, z0, tu1, tv0, light, tint, 0, 1, 0);
            }
        }
    }

    private void drawWalls(VertexConsumer vc, PoseStack ps, TextureAtlasSprite sprite,
                           float top, int light, int tint) {
        float a = INSET, b = 1.0f - INSET;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0();
        // Only sample as much of the sprite vertically as the column actually fills.
        float v1 = Mth.lerp((top - POOL_BOTTOM) / (POOL_TOP - POOL_BOTTOM), sprite.getV0(), sprite.getV1());

        // north (-Z)
        quad(vc, ps, a, POOL_BOTTOM, a, b, top, a, u0, v0, u1, v1, light, tint, 0, 0, -1);
        // south (+Z)
        quad(vc, ps, b, POOL_BOTTOM, b, a, top, b, u0, v0, u1, v1, light, tint, 0, 0, 1);
        // west (-X)
        quad(vc, ps, a, POOL_BOTTOM, b, a, top, a, u0, v0, u1, v1, light, tint, -1, 0, 0);
        // east (+X)
        quad(vc, ps, b, POOL_BOTTOM, a, b, top, b, u0, v0, u1, v1, light, tint, 1, 0, 0);
    }

    // ------------------------------------------------------------------

    private static void vertex(VertexConsumer vc, PoseStack ps,
                               float x, float y, float z, float u, float v,
                               int light, int color, float nx, float ny, float nz) {
        vc.addVertex(ps.last().pose(), x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(ps.last(), nx, ny, nz);
    }

    private static void quad(VertexConsumer vc, PoseStack ps,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float u0, float v0, float u1, float v1,
                             int light, int color, float nx, float ny, float nz) {
        vertex(vc, ps, x0, y0, z0, u0, v0, light, color, nx, ny, nz);
        vertex(vc, ps, x0, y1, z1, u0, v1, light, color, nx, ny, nz);
        vertex(vc, ps, x1, y1, z1, u1, v1, light, color, nx, ny, nz);
        vertex(vc, ps, x1, y0, z0, u1, v0, light, color, nx, ny, nz);
    }
}
