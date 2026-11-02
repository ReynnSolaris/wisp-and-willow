package com.reynn.wispwillow.client.goggles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.client.renderers.FluxArcRenderer;
import com.reynn.wispwillow.client.renderers.OverlayShapes;
import com.reynn.wispwillow.entity.OverlayHostEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Draws the Fluxia currents and, for a goggle wearer, the node gauges and links, from the
 * {@link OverlayHostEntity} that rides along with the player.
 *
 * <p>The dispatcher puts the pose at the entity's own position; this moves it back to the
 * world origin so the shapes can keep speaking in world coordinates, then draws everything
 * into the entity buffer source, which is flushed with the rest of the translucent entity
 * geometry — the path a shader pack renders.</p>
 */
public class OverlayHostRenderer extends EntityRenderer<OverlayHostEntity> {

    public OverlayHostRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /** Never culled: what it draws is spread across the world, not sat where the entity is. */
    @Override
    public boolean shouldRender(OverlayHostEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(OverlayHostEntity entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        List<FluxArcRenderer.FlowView> flows = FluxArcRenderer.extract(entity.level(), partialTick);
        Camera cam = mc.gameRenderer.getMainCamera();
        List<ArcaneOverlayRenderer.NodeView> nodes = ArcaneVision.active()
                ? ArcaneOverlayRenderer.extract(mc, entity.level(), cam.getPosition())
                : List.of();
        if (flows.isEmpty() && nodes.isEmpty()) return;

        float time = entity.level().getGameTime() + partialTick;

        // The dispatcher has put (0,0,0) at the entity's interpolated position; undo that.
        Vec3 at = entity.getPosition(partialTick);
        ps.pushPose();
        ps.translate(-at.x, -at.y, -at.z);
        FluxArcRenderer.submit(flows, ps, buf, cam, time);
        ArcaneOverlayRenderer.submit(nodes, ps, buf, cam, time, mc.font);
        ps.popPose();
    }

    /** Required by the base class; nothing here draws with it. */
    @Override
    public ResourceLocation getTextureLocation(OverlayHostEntity entity) {
        return OverlayShapes.BEAM_TEX;
    }
}
