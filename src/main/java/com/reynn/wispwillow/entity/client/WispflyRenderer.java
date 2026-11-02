package com.reynn.wispwillow.entity.client;

import com.reynn.wispwillow.entity.wild.WispflyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Drawn at full block light so it reads as a light in the dark, with the lantern on the glow
 * mask on top of that so it stays lit even under a shader pack's own lighting.
 *
 * <p>The light level is used rather than an additive render type so a shader pack treats a
 * wispfly like any other cutout entity instead of dropping it.</p>
 */
public class WispflyRenderer extends GeoEntityRenderer<WispflyEntity> {

    private static final ResourceLocation TEXTURE = WWGeoModel.sheet("wildlife", "wispfly");

    public WispflyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WWGeoModel<>("wispfly", e -> TEXTURE, null, 0f, "head", 0.5f));
        this.shadowRadius = 0.1f;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected int getBlockLightLevel(WispflyEntity entity, BlockPos pos) {
        return 15;
    }
}
