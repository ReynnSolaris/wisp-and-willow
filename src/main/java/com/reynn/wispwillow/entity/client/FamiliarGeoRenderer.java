package com.reynn.wispwillow.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reynn.wispwillow.entity.familiar.FamiliarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The renderer the four generated familiars share: a GeckoLib model, a shadow, and the shrink
 * every familiar does while it is being banished. Only the banishing flag is synced, not how
 * far through the dissolve it is, so the shrink is a constant and the entity's own rise, spin
 * and particles carry the rest.
 */
public class FamiliarGeoRenderer<T extends FamiliarEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    /** How far it shrinks while dissolving, matching the raven. */
    private static final float BANISH_SCALE = 0.6f;

    public FamiliarGeoRenderer(EntityRendererProvider.Context ctx, GeoModel<T> model, float shadow) {
        super(ctx, model);
        this.shadowRadius = shadow;
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, T entity,
                                    BakedGeoModel model, boolean isReRender, float partialTick,
                                    int packedLight, int packedOverlay) {
        if (entity.isBanishing()) {
            widthScale *= BANISH_SCALE;
            heightScale *= BANISH_SCALE;
        }
        super.scaleModelForRender(widthScale, heightScale, poseStack, entity, model, isReRender,
                partialTick, packedLight, packedOverlay);
    }
}
