package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.alchemy.AlchemicalCauldronBlockEntity;
import com.reynn.wispwillow.blocks.alchemy.CauldronPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * The part that makes it look mythical rather than industrial.
 *
 * Three layers, all cheap:
 *   1. the brew surface inside the basin, using the real fluid sprite
 *   2. a ring of runes orbiting above the mouth, faster while a brew is running
 *   3. a soft vertical shaft of light, only while actually crafting
 *
 * Everything keys off LIT, so an unformed or unpaid cauldron renders as a plain pot and
 * the player can tell the two apart from across the room.
 */
public class AlchemicalCauldronRenderer implements BlockEntityRenderer<AlchemicalCauldronBlockEntity> {

    private static final ResourceLocation RUNE_RING = ResourceLocation.fromNamespaceAndPath(
            WispWillow.MODID, "textures/block/cauldron_rune_ring.png");

    private static final float BASIN_TOP = 0.72f;     // inner fluid height, in block units
    // Just clear of the rim. High enough to read as floating, low enough that you are
    // looking at the pot and the ring together rather than craning at the ceiling.
    private static final float RING_HEIGHT = 1.15f;

    public AlchemicalCauldronRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(AlchemicalCauldronBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {

        if (!be.isLit()) return;

        long time = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        float t = time + partialTick;
        // The block entity tells us whether a brew is actually advancing. Reading "the
        // reagent tank has something in it" instead would light the shaft over a pot that
        // is stalled for power, jammed on a full output, or simply holding the wrong fluid.
        boolean brewing = be.isCrafting();

        renderBrewSurface(be, pose, buffers, light, t);
        renderRuneRing(pose, buffers, t, brewing);
        if (brewing) renderShaft(pose, buffers, t);
    }

    private void renderBrewSurface(AlchemicalCauldronBlockEntity be, PoseStack pose,
                                   MultiBufferSource buffers, int light, float t) {
        FluidStack fluid = be.inputTank().getFluid();
        if (fluid.isEmpty()) return;

        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);

        float fill = fluid.getAmount() / (float) be.inputTank().getCapacity();
        // Never render a surface flush with the rim; a sliver of basin reads as depth.
        float y = 0.28f + (BASIN_TOP - 0.28f) * Mth.clamp(fill, 0.05f, 1f);
        // Gentle bob so a full pot does not look like a frozen texture.
        y += Mth.sin(t * 0.06f) * 0.004f;

        pose.pushPose();
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f mat = pose.last().pose();

        float a = ((tint >> 24) & 0xFF) / 255f;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >> 8) & 0xFF) / 255f;
        float b = (tint & 0xFF) / 255f;
        if (a == 0f) a = 1f;

        float x0 = 0.185f, x1 = 0.815f;
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        vc.addVertex(mat, x0, y, x0).setColor(r, g, b, a).setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(mat, x0, y, x1).setColor(r, g, b, a).setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(mat, x1, y, x1).setColor(r, g, b, a).setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        vc.addVertex(mat, x1, y, x0).setColor(r, g, b, a).setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);

        pose.popPose();
    }

    private void renderRuneRing(PoseStack pose, MultiBufferSource buffers, float t, boolean brewing) {
        float speed = brewing ? 0.030f : 0.008f;
        float spin = (t * speed) % (Mth.PI * 2f);
        float bob = Mth.sin(t * 0.04f) * 0.03f;

        pose.pushPose();
        pose.translate(0.5f, RING_HEIGHT + bob, 0.5f);
        pose.mulPose(com.mojang.math.Axis.YP.rotation(spin));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
        pose.scale(1.6f, 1.6f, 1.6f);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(RUNE_RING));
        Matrix4f mat = pose.last().pose();
        float s = 0.5f;
        // An idle ring at 0.45 washed out to nearly nothing against a bright sky. Both
        // states stay solid; brewing reads by the spin-up and the shaft, not by opacity.
        float alpha = brewing ? 1.0f : 0.8f;

        vc.addVertex(mat, -s, -s, 0).setColor(1f, 1f, 1f, alpha).setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        vc.addVertex(mat, -s, s, 0).setColor(1f, 1f, 1f, alpha).setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        vc.addVertex(mat, s, s, 0).setColor(1f, 1f, 1f, alpha).setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
        vc.addVertex(mat, s, -s, 0).setColor(1f, 1f, 1f, alpha).setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);

        pose.popPose();
    }

    private void renderShaft(PoseStack pose, MultiBufferSource buffers, float t) {
        pose.pushPose();
        pose.translate(0.5f, BASIN_TOP, 0.5f);

        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f mat = pose.last().pose();

        float w = 0.10f + Mth.sin(t * 0.12f) * 0.015f;
        float h = RING_HEIGHT - BASIN_TOP;

        // Two crossed quads. Cheaper than a cylinder and reads the same at any angle.
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(i * 90f));
            Matrix4f m = pose.last().pose();
            vc.addVertex(m, -w, 0, 0).setColor(0.78f, 0.42f, 0.88f, 0.55f);
            vc.addVertex(m, -w, h, 0).setColor(0.90f, 0.75f, 1.00f, 0.05f);
            vc.addVertex(m, w, h, 0).setColor(0.90f, 0.75f, 1.00f, 0.05f);
            vc.addVertex(m, w, 0, 0).setColor(0.78f, 0.42f, 0.88f, 0.55f);
            pose.popPose();
        }

        pose.popPose();
    }

    /** Keep rendering the ring from a distance; it is the main "this is running" tell. */
    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public boolean shouldRenderOffScreen(AlchemicalCauldronBlockEntity be) {
        return true;
    }

    /** Wide enough for the whole shrine, so nothing pops out at the edge of the frustum. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(AlchemicalCauldronBlockEntity be) {
        Vec3 c = Vec3.atCenterOf(be.getBlockPos());
        double r = CauldronPattern.RADIUS + 0.5;
        return new net.minecraft.world.phys.AABB(c.x - r, c.y - 1.5, c.z - r,
                c.x + r, c.y + 3.5, c.z + r);
    }
}
