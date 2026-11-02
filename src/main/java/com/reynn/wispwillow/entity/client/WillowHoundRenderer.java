package com.reynn.wispwillow.entity.client;

import com.reynn.wispwillow.entity.wild.WillowHoundEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * One coat for tame and wild alike: taming a hound changes who it follows, not what it is.
 * The amber eyes are on the glow mask, so they burn in the dark.
 */
public class WillowHoundRenderer extends GeoEntityRenderer<WillowHoundEntity> {

    private static final ResourceLocation TEXTURE = WWGeoModel.sheet("wildlife", "willow_hound");

    public WillowHoundRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<>("willow_hound", e -> TEXTURE, "neck", 0.4f, "head", 0.6f));
        this.shadowRadius = 0.4f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
