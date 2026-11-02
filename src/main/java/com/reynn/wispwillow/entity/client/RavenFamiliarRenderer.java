package com.reynn.wispwillow.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.entity.RavenFamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * One texture, because there is exactly one raven and it is not a set.
 *
 * <p>The hue cycle the entity computes for its particles is picked up here and used as the
 * render tint, so the bird catches the same colour as the swirl around it rather than sitting
 * flat black in the middle of a rainbow. It is blended toward white first: a fully saturated
 * tint over a painted texture reads as a colour-swap, a partial one as a sheen.</p>
 */
public class RavenFamiliarRenderer extends GeoEntityRenderer<RavenFamiliarEntity> {

    /** How far it shrinks while dissolving, matching the other familiars. */
    private static final float BANISH_SCALE = 0.6f;

    /** How much of the shimmer survives the blend toward white. */
    private static final float SHIMMER_STRENGTH = 0.6f;

    public RavenFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RavenFamiliarGeoModel());
        this.shadowRadius = 0.25f;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    RavenFamiliarEntity raven, BakedGeoModel model, boolean isReRender,
                                    float partialTick, int packedLight, int packedOverlay) {
        if (raven.isBanishing()) {
            widthScale *= BANISH_SCALE;
            heightScale *= BANISH_SCALE;
        }
        super.scaleModelForRender(widthScale, heightScale, poseStack, raven, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }

    @Override
    public Color getRenderColor(RavenFamiliarEntity raven, float partialTick, int packedLight) {
        int shimmer = raven.shimmerColour(partialTick);
        return Color.ofRGB(
                blend((shimmer >> 16) & 0xFF),
                blend((shimmer >> 8) & 0xFF),
                blend(shimmer & 0xFF));
    }

    private static int blend(int channel) {
        return Math.round(channel * SHIMMER_STRENGTH + 255f * (1f - SHIMMER_STRENGTH));
    }
}
