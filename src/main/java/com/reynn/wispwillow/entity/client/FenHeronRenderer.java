package com.reynn.wispwillow.entity.client;

import com.reynn.wispwillow.entity.wild.FenHeronEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Babies need nothing here: GeckoLib's entity renderer already halves a baby.
 * The look is split across the two upper neck joints so the head turns on an S, not a hinge.
 */
public class FenHeronRenderer extends GeoEntityRenderer<FenHeronEntity> {

    private static final ResourceLocation TEXTURE = WWGeoModel.sheet("wildlife", "fen_heron");

    public FenHeronRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<>("fen_heron", e -> TEXTURE, "neck2", 0.45f, "head", 0.55f));
        this.shadowRadius = 0.45f;
    }
}
