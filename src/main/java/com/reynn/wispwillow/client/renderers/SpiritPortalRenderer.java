package com.reynn.wispwillow.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.blocks.spirit.SpiritPortalBlock;
import com.reynn.wispwillow.blocks.spirit.SpiritPortalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the Open Way's surface: a violet dusk seen through a doorway, rising rather than
 * falling and shifting with the camera, with a drift of pale sparks over it and a lilac rim
 * burning around the outline of the gate.
 *
 * <p>Built on {@link PortalSurface}, which it shares with {@link GatewayPortalRenderer} and
 * for the same reasons — stock render types only, so a shader pack cannot lose it. The two
 * differences are deliberate: this field drifts <em>upward</em>, because the way out of the
 * world should not look like something draining, and the layers are tinted toward the realm's
 * violet so the gate matches the place it opens onto.</p>
 */
public class SpiritPortalRenderer implements BlockEntityRenderer<SpiritPortalBlockEntity> {

    /**
     * The gate's artwork. One seamless square tile, opaque, with the bright marks on top —
     * the deepest layer is drawn at full alpha and is what hides whatever is behind the sheet.
     */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WispWillow.MODID, "textures/block/spirit_portal_field.png");

    private static final PortalSurface.Style STYLE = new PortalSurface.Style(
            TEXTURE,
            new float[]{1.0f, 1.6f, 2.4f},
            new float[]{0.05f, 0.16f, 0.30f},
            /* negative: the field climbs */
            new float[]{-0.008f, -0.017f, -0.029f},
            new int[]{0xFFFFFFFF, 0x99E2D4FF, 0x5EBFA8FF},
            0xCCF0E6FF,
            0xB8C9A6FF);

    public SpiritPortalRenderer(BlockEntityRendererProvider.Context ctx) {}

    /** Wide enough for the corner block to draw the whole opening without being culled. */
    @Override
    public AABB getRenderBoundingBox(SpiritPortalBlockEntity be) {
        return PortalSurface.renderBox(be.getBlockPos());
    }

    @Override
    public void render(SpiritPortalBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!state.hasProperty(SpiritPortalBlock.AXIS) || be.getLevel() == null) return;
        boolean acrossX = state.getValue(SpiritPortalBlock.AXIS) == Direction.Axis.X;
        PortalSurface.render(STYLE, be.getLevel(), be.getBlockPos(), state.getBlock(), acrossX, ps, buf, partialTick);
    }
}
