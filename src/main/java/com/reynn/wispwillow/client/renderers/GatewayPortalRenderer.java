package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.fluxia.GatewayPortalBlock;
import com.reynn.wispwillow.blocks.fluxia.GatewayPortalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Soul Gateway's surface: smoke over a violet void threaded with amber, sliding
 * downward and shifting with the camera, with gold sparks drifting up through it and a gilt
 * rim burning around the opening.
 *
 * <p>Deliberately <em>not</em> vanilla's {@code RenderType.endPortal()}. That looked right in
 * vanilla, but a shader pack maps the end-portal program onto its own gbuffers and loses the
 * parallax, leaving a flat sheet of the vanilla end sky. It also baked its two textures into
 * the render type, so there was no way to use our own art. The drawing itself lives in
 * {@link PortalSurface}, shared with the Open Way.</p>
 */
public class GatewayPortalRenderer implements BlockEntityRenderer<GatewayPortalBlockEntity> {

    /**
     * The gateway's artwork. One seamless square tile.
     *
     * <p>The renderer supplies the motion, so this file is a still image, not a frame strip —
     * a {@code .mcmeta} animation would do nothing here. Only atlas sprites animate from
     * mcmeta, and this is bound as a standalone texture; the layers scroll it instead, which
     * is also what tiling it across the opening depends on.</p>
     *
     * <p>Wants an <b>opaque background</b> with the bright marks on top of it: the deepest
     * layer is drawn at full alpha and is what hides the willow frame behind the gateway.</p>
     */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/block/soul_gateway_field.png");

    private static final PortalSurface.Style STYLE = new PortalSurface.Style(
            TEXTURE,
            new float[]{1.0f, 1.7f, 2.6f},
            new float[]{0.05f, 0.17f, 0.32f},
            new float[]{0.010f, 0.021f, 0.036f},
            new int[]{0xFFFFFFFF, 0x99FFFFFF, 0x5EFFFFFF},
            0xCCFFE2A0,
            0xC0FFC85A);

    public GatewayPortalRenderer(BlockEntityRendererProvider.Context ctx) {}

    /** Wide enough for the corner block to draw the whole opening without being culled. */
    @Override
    public AABB getRenderBoundingBox(GatewayPortalBlockEntity be) {
        return PortalSurface.renderBox(be.getBlockPos());
    }

    @Override
    public void render(GatewayPortalBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(GatewayPortalBlock.AXIS) || be.getLevel() == null) return;
        boolean acrossX = state.getValue(GatewayPortalBlock.AXIS) == Direction.Axis.X;
        PortalSurface.render(STYLE, be.getLevel(), be.getBlockPos(), state.getBlock(), acrossX, ps, buf, partialTick);
    }
}
