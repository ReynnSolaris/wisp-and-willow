package com.reynn.wispwillow.entity.client;

import com.reynn.wispwillow.entity.spirit.GlimmerfawnEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The shorn state is a hidden bone, not a second texture, so there is only one sheet: the
 * {@code coat} bone carries the whole fleece and is switched off while the fawn is sheared.
 * The motes in the coat and the star on the brow are on the glow mask.
 */
public class GlimmerfawnRenderer extends GeoEntityRenderer<GlimmerfawnEntity> {

    private static final ResourceLocation TEXTURE = WWGeoModel.sheet("spirit", "glimmerfawn");

    public GlimmerfawnRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<GlimmerfawnEntity>("glimmerfawn", e -> TEXTURE, "neck", 0.5f, "head", 0.5f) {
            @Override
            public void setCustomAnimations(GlimmerfawnEntity fawn, long instanceId,
                                            AnimationState<GlimmerfawnEntity> state) {
                super.setCustomAnimations(fawn, instanceId, state);
                setHidden("coat", fawn.isSheared());
            }
        });
        this.shadowRadius = 0.45f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
